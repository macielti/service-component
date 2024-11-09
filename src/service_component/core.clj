(ns service-component.core
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [io.pedestal.http :as http]))

(defmethod ig/init-key ::service
  [_ {:keys [components]}]
  (log/info :starting ::service)
  (http/start (http/create-server {::http/routes          (:routes components)
                                   ::http/allowed-origins (constantly true)
                                   ::http/host            (-> components :config :service :host)
                                   ::http/port            (-> components :config :service :port)
                                   ::http/type            :jetty
                                   ::http/join?           false})))

(defmethod ig/halt-key! ::service
  [_ service]
  (log/info :stopping ::service)
  (http/stop service))
