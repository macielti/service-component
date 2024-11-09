(ns service-component-test
  (:require [clojure.test :refer :all]
            [integrant.core :as ig]
            [schema.test :as s]
            [io.pedestal.test :as test]
            [matcher-combinators.test :refer [match?]]
            [common-clj.integrant-components.config :as component.config]
            [common-clj.integrant-components.routes :as component.routes]
            [service-component.core :as component.service]))

(def routes [["/test" :get [(fn [{{:keys [config]} :components}]
                              {:status 200
                               :body   config})]
              :route-name :test]])

(def system-components
  {::component.config/config   {:path "test/resources/config.example.edn"
                                :env  :test}
   ::component.routes/routes   {:routes routes}
   ::component.service/service {:components {:config (ig/ref ::component.config/config)
                                             :routes (ig/ref ::component.routes/routes)}}})

(s/deftest service-component-test
  (let [system (ig/init system-components)
        service-fn (-> system ::component.service/service :io.pedestal.http/service-fn)]

    (testing "That we can fetch the test endpoint and access components from the request"
      (is (match? {:status 200
                   :body   "{\"service-name\":\"rango\",\"service\":{\"host\":\"0.0.0.0\",\"port\":8080},\"current-env\":\"test\"}"}
                  (test/response-for service-fn :get "/test" :headers {"authorization" "Bearer test-token"}))))

    (ig/halt! system)))
