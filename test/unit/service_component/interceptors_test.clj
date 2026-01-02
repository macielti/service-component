(ns service-component.interceptors-test
  (:require [clojure.test :refer :all]
            [io.pedestal.interceptor.chain :as chain]
            [matcher-combinators.test :refer [match?]]
            [schema.core]
            [schema.test :as s]
            [service-component.interceptors :as interceptors])
  (:import (clojure.lang ExceptionInfo)))

(schema.core/defschema QueryParams
  {:hello schema.core/Str})

(schema.core/defschema QueryParamsOptionalKey
  {(schema.core/optional-key :hello) schema.core/Str})

(s/deftest query-params-schema-test
  (let [ex (is (thrown? ExceptionInfo (chain/execute {} [(interceptors/query-params-schema QueryParams)])))]
    (is (match? {:status  422
                 :error   "invalid-payload-for-query-params"
                 :message "The system detected that the received data is invalid."
                 :detail  "{:hello (not (instance? java.lang.String nil))}"}
                (ex-data ex))))

  (is (match? {:request {:query-params {:hello "world"}}}
              (chain/execute {:request {:query-params {:hello "world"}}} [(interceptors/query-params-schema QueryParams)])))

  (is (match? {:request {:query-params {:hello "world"}}}
              (chain/execute {:request {:query-params {:hello :world}}} [(interceptors/query-params-schema QueryParams)])))

  (is (match? {:request {:query-params {}}}
              (chain/execute {:request {}} [(interceptors/query-params-schema QueryParamsOptionalKey)]))))
