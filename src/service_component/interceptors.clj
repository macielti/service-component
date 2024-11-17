(ns service-component.interceptors
  (:require [clojure.tools.logging :as log]
            [humanize.schema :as h]
            [iapetos.core :as prometheus]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.interceptor :as pedestal.interceptor]
            [io.pedestal.interceptor.error :as error]
            [schema.core :as s]
            [service-component.error :as common-error])
  (:import (clojure.lang ExceptionInfo)))

#_{:clj-kondo/ignore [:unresolved-symbol]}
(def error-handler-interceptor
  (error/error-dispatch [ctx ex]
                        [{:exception-type :clojure.lang.ExceptionInfo}]
                        (let [{:keys [status error message detail]} (ex-data ex)]
                          (assoc ctx :response {:status status
                                                :body   {:error   error
                                                         :message message
                                                         :detail  detail}}))

                        :else
                        (do (log/error ex)
                            (assoc ctx :response {:status 500 :body {:error   "unexpected-server-error"
                                                                     :message "Internal Server Error"
                                                                     :detail  "Internal Server Error"}}))))

(defn components-interceptor [system-components]
  (pedestal.interceptor/interceptor
    {:name  ::components-interceptor
     :enter (fn [context]
              (assoc-in context [:request :components] system-components))}))

(defn default-interceptors [components]
  [(body-params/body-params)
   (components-interceptor components)
   http/json-body
   error-handler-interceptor])

(defn schema-body-in-interceptor [schema]
  (pedestal.interceptor/interceptor {:name  ::schema-body-in-interceptor
                                     :enter (fn [{{:keys [json-params]} :request :as context}]
                                              (try (s/validate schema json-params)
                                                   (catch ExceptionInfo e
                                                     (when (= (:type (ex-data e)) :schema.core/error)
                                                       (common-error/http-friendly-exception 422
                                                                                             "invalid-schema-in"
                                                                                             "The system detected that the received data is invalid"
                                                                                             (get-in (h/ex->err e) [:unknown :error])))))
                                              context)}))

(def http-request-in-handle-timing-interceptor
  (pedestal.interceptor/interceptor
    {:name  ::http-request-in-handle-timing
     :enter (fn [context]
              (assoc context ::start-ms (System/currentTimeMillis)))
     :leave (fn [{{:keys [components]} :request :as context}]
              (let [{::keys [start-ms]} context
                    prometheus-registry (get-in components [:prometheus :registry])
                    service-name (get-in components [:config :service-name])
                    elapsed-ms (- (System/currentTimeMillis) start-ms)
                    route-name (get-in context [:route :route-name])]
                (prometheus/observe prometheus-registry :http-request-in-handle-timing-v2
                                    {:service  (name service-name)
                                     :endpoint (name route-name)}
                                    elapsed-ms)
                (dissoc context ::start-ms)))}))
