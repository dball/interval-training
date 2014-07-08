(ns interval-training.ticker
  (:require [clojure.core.async :as a])
  (:import [java.util.concurrent Executors ScheduledExecutorService TimeUnit]))

(defn ^ScheduledExecutorService scheduler
  []
  (Executors/newSingleThreadScheduledExecutor))

(defn ticker-chan
  [period]
  (let [c (a/chan (a/sliding-buffer 1))
        scheduler (scheduler)
        tick (atom 0)
        task #(a/go
                (when-not (a/>! c (swap! tick inc))
                  (.shutdown ^ScheduledExecutorService scheduler)))]
    (.scheduleAtFixedRate scheduler task 0 period TimeUnit/SECONDS)
    c))
