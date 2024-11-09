(ns service-component.interceptors
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.body-params :as body-params]
   [io.pedestal.interceptor :as pedestal.interceptor]))

(defn components-interceptor [system-components]
  (pedestal.interceptor/interceptor
   {:name  ::components-interceptor
    :enter (fn [context]
             (assoc-in context [:request :components] system-components))}))

(defn default-interceptors [components]
  [(body-params/body-params)
   (components-interceptor components)
   http/json-body])
