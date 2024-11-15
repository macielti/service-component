(ns service-component-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [is testing]]
            [common-clj.integrant-components.config :as component.config]
            [common-clj.integrant-components.routes :as component.routes]
            [integrant.core :as ig]
            [io.pedestal.test :as test]
            [matcher-combinators.test :refer [match?]]
            [schema.core :as schema]
            [schema.test :as s]
            [service-component.core :as component.service]
            [service-component.interceptors :as interceptors]))

(def routes [["/test" :get [(fn [{{:keys [config]} :components}]
                              {:status 200
                               :body   config})]
              :route-name :test]
             ["/schema-validation-interceptor-test" :post [(interceptors/schema-body-in-interceptor {:test schema/Str})
                                                           (fn [_]
                                                             {:status 200
                                                              :body   {:test :schema-ok}})]
              :route-name :schema-validation-interceptor-test]])

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

    (testing "That we can't fetch the test endpoint without a valid schema"
      (is (match? {:status 422
                   :body   "{\"error\":\"invalid-schema-in\",\"message\":\"The system detected that the received data is invalid\",\"detail\":{\"test\":\"missing-required-key\"}}"}
                  (test/response-for service-fn :post "/schema-validation-interceptor-test"
                                     :headers {"Content-Type" "application/json"}
                                     :body (json/encode {}))))

      (is (match? {:status 422
                   :body   "{\"error\":\"invalid-schema-in\",\"message\":\"The system detected that the received data is invalid\",\"detail\":null}"}
                  (test/response-for service-fn :post "/schema-validation-interceptor-test")))

      (is (match? {:status 200
                   :body   "{\"test\":\"schema-ok\"}"}
                  (test/response-for service-fn :post "/schema-validation-interceptor-test"
                                     :headers {"Content-Type" "application/json"}
                                     :body (json/encode {:test :ok})))))

    (ig/halt! system)))
