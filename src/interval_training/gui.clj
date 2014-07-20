(ns interval-training.gui
  (:require [clojure.core.async :as a]
            [seesaw.core :refer :all]
            [seesaw.font :refer :all]))

(defmulti display-message!
  (fn [ui message]
    (cond
     (string? message)
     :string

     (integer? message)
     :integer)))

(defmethod display-message! :string
  [ui activity]
  (config! (:activity ui) :text activity)
  ui)

(defmethod display-message! :integer
  [ui time]
  (let [minutes (int (/ time 60))
        seconds (mod time 60)
        label (format "%02d:%02d" minutes seconds)]
    (config! (:stopwatch ui) :text label))
  ui)

(defn build-ui
  []
  (let [f (frame :title "Interval Training" :width 800 :height 600)
        stopwatch (label :text ""
                         :font (font :name :monospaced
                                     :size 240)
                         :halign :center)
        activity (label :text ""
                        :font (font :name "Lucida Grande"
                                    :size 80)
                        :halign :center)
        split (top-bottom-split stopwatch activity :divider-location 0.8 :preferred-size [800 :by 600])]
    
    (config! f :content split)
    {:frame f
     :split split
     :stopwatch stopwatch
     :activity activity}))

(defn start!
  []
  (native!)
  (let [ui (-> (build-ui)
               (display-message! 0))
        {:keys [frame]} ui]
    (-> frame pack! show!)
    ui))

(defn stop!
  [ui]
  (dispose! (:frame ui))
  nil)

(defn display-chan
  []
  (let [ui (start!)
        c (a/chan)
        work (a/go-loop []
               (when-let [message (a/<! c)]
                 (display-message! ui message)
                 (recur)))
        process (a/go (a/<! work)
                      (stop! ui))]
    c))
