(ns interval-training.speaker
  (:require [clojure.core.async :as a]
            [clojure.java.shell :refer [sh]])
  (:import
   (com.sun.speech.freetts Voice)
   (com.sun.speech.freetts VoiceManager)
   (com.sun.speech.freetts.audio JavaClipAudioPlayer)
   (com.sun.speech.freetts.audio JavaStreamingAudioPlayer)
   (javax.sound.sampled AudioFileFormat$Type)))

(defprotocol Speaker
  (speak [_ utterance]))

(declare default-speaker)

(defn speaker-chan
  "Returns a channel which will speak the messages given to it"
  ([] (speaker-chan default-speaker))
  ([speaker]
     (let [c (a/chan)]
       (a/go-loop []
                  (when-let [utterance (a/<! c)]
                    (speak speaker utterance)
                    (recur)))
       c)))

(defrecord FreeTTSSpeaker [dir voice]
  Speaker
  (speak [_ utterance]
    (System/setProperty "freetts.voices" dir)
    (let [voice-manager (. VoiceManager getInstance)
          audioplayer (doto (new JavaStreamingAudioPlayer))]
      (doto (. voice-manager getVoice voice)
        (.allocate)
        (.setAudioPlayer audioplayer)
        (.speak utterance)
        (.deallocate))
      (.close audioplayer))
    nil))

(def kevin-speaker
  (->FreeTTSSpeaker "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory"
                    "kevin16"))

(def osx-voices
  {:agnes "Agnes"
   :kathy "Kathy"
   :princess "Princess"
   :vicki "Vicki"
   :victoria "Victoria"

   :bruce "Bruce"
   :fred "Fred"
   :junior "Junior"
   :ralph "Ralph"
   :alex "Alex"                     ; probably most realistic sounding

   :albert "Albert"
   :zarvox "Zarvox"
   :trinoids "Trinoids"
   :whisper "Whisper"

   :bahh "Bahh"
   :boing "Boing"
   :bubbles "Bubbles"
   :bad-news "Bad News"
   :good-news "Good News"
   :deranged "Deranged"
   :hysterical "Hysterical"

   :bells "Bells"
   :cellos "Cellos"
   :pipe-organ "Pipe Organ"})

(defrecord OSXSpeaker [voice]
  Speaker
  (speak [_ utterance]
    (let [osx-voice (get osx-voices voice voice)]
      (sh "say" "-v" osx-voice utterance))
    nil))

(def alex-osx-speaker
  (->OSXSpeaker :alex))

(def default-speaker alex-osx-speaker)
