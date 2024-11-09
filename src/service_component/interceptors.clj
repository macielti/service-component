(ns service-component.interceptors
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.interceptor :as pedestal.interceptor]
            [humanize.schema :as h]
            [schema.core :as s])
  (:import (clojure.lang ExceptionInfo)))

(defn components-interceptor [system-components]
  (pedestal.interceptor/interceptor
    {:name  ::components-interceptor
     :enter (fn [context]
              (assoc-in context [:request :components] system-components))}))

(defn default-interceptors [components]
  [(body-params/body-params)
   (components-interceptor components)
   http/json-body])

(defn schema-body-in-interceptor [schema]
  (pedestal.interceptor/interceptor
    {:name  ::schema-body-in-interceptor
     :enter (fn [{{:keys [json-params]} :request :as context}]
              (let [validation-result (try (s/validate schema json-params)
                                           (catch ExceptionInfo e
                                             (when (= (:type (ex-data e)) :schema.core/error)
                                               {:status 422
                                                :body   {:error "invalid-params"
                                                         :data  (get-in (h/ex->err e) [:unknown :error])}})))]
                (if (and (= "invalid-params" (get-in validation-result [:body :error]))
                         (422 (:status validation-result)))
                  (assoc context :response validation-result)
                  context)))}))
