(ns commiteth.eth.multisig-wallet
  (:require [commiteth.eth.core :as eth]
            [clojure.tools.logging :as log]))

(defonce methods
  {:submit-transaction (eth/sig->method-id "submitTransaction(address,uint256,bytes)")
   :withdraw-everything (eth/sig->method-id "withdrawEverything(address)")
   :token-balances (eth/sig->method-id "tokenBalances(address)")
   :get-token-list (eth/sig->method-id "getTokenList()")
   :create (eth/sig->method-id "create(address[],uint256)")
   :watch (eth/sig->method-id "watch(address,bytes)")})

(defonce topics
  {:factory-create (eth/event-sig->topic-id "Create(address,address)")
   :submission (eth/event-sig->topic-id "Submission(uint256)")})

(defonce factory-contract-addr "0xbcBc5b8cE5c76Ed477433636926f76897401f838")

(defn create-new
  [owner1 owner2 required]
  (eth/execute (eth/eth-account)
               factory-contract-addr
               (:create methods)
               0x40
               0x2
               required
               owner1
               owner2))

(defn execute
  [contract to value]
  (log/debug "multisig.execute(contract, to, value)" contract to value)
  (eth/execute (eth/eth-account)
               contract
               (:submit-transaction methods)
               to
               value
               "0x60"
               "0x0"
               "0x0"))

(defn find-confirmation-hash
  [receipt]
  (let [logs                   (:logs receipt)
        confirmation-topic? (fn [topic]
                              (= topic
                                 (:submission topics)))
        has-confirmation-event? #(some confirmation-topic?
                                      (:topics %))
        confirmation-event     (first (filter has-confirmation-event? logs))
        confirmation-data      (:data confirmation-event)]
    (when confirmation-data
      (subs confirmation-data 2 66))))

(defn find-factory-hash
  [receipt]
  (let [logs                   (:logs receipt)
        factory-topic? (fn [topic]
                         (= topic
                            (:factory-create topics)))
        has-factory-event? #(some factory-topic?
                                 (:topics %))
        factory-event     (first (filter has-factory-event? logs))
        factory-data      (:data factory-event)]
    (when factory-data
      (subs factory-data 2 66))))


(defn send-all
  [contract to]
  (log/debug "multisig.send-all(contract, to)" contract to)
  (let [params (eth/format-call-params
                (:withdraw-everything methods)
                to)]
    (eth/execute (eth/eth-account)
                 contract
                 (:submit-transaction methods)
                 contract
                 0
                 "0x60"
                 "0x24"
                 params)))


(defn watch-token
  [contract token]
  (log/debug "multisig.watch-token(contract, token)" contract token)
  (eth/execute (eth/eth-account)
               contract
               (:watch methods)
               token
               0))

(defn token-balance
  [contract token]
  (eth/call contract (:token-balances methods) token))

(defn tokens-list
  [contract]
  (eth/call contract (:get-token-list methods)))
