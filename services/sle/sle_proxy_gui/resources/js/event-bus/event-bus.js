/**
 * Simple pub/sub message bus used for component communication. 
 */
var EventBus = (function() {
  var topics = {};
  
  /**
   * Subscribe to topic
   * @param {String} topic - topic to subscribe to
   * @param {Callback} listener - callback function
   */
  function subscribe(topic, listener) {
    // create the topic if not yet created
    if(!topics[topic]) topics[topic] = [];

    // add the listener
    topics[topic].push(listener);
  }

  /**
   * Publish to topic.
   * @param {String} topic - topic to publish to
   * @param {Object} data - data object sent to the topic
   */
  function publish(topic, data) {
    // return if the topic doesn't exist, or there are no listeners
    if(!topics[topic] || topics[topic].length < 1) return;

    // send the event to all listeners
    topics[topic].forEach(function(listener) {
      listener(data || {});
    });
  }
  
  return {
	  publish: publish,
	  subscribe: subscribe
  }
  
})();