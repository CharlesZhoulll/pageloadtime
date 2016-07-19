# pageloadtime

This is a small system working on client side (your Android Phone !) that collects page loading time of any website 
you interest.

Some pitfalls in PLT measurement.

Do not try to using wget or similar library to directly download the webpage. The results has zero coloration with the real page load latency. 

The better way is to use Navigation Timing API. However there are many confusing parts in its documents. Especially the attributes of responseEnd , domContentLoadedEventEnd, domComplete, etc.

According to the documentation, responseEnd is the time when receive the last byte from server. This is terribly confusing, because it only accounts for the static html page. So if you take the duration, you might end up with the similar results as using wget.

The first meaningful attribute is  domContentLoadedEventEnd, this denote that the basic DOM is constructed and the user can start interactive with the webpage. And this is also what called as PIT.

Notice that domComplete is very different with domContentLoadedEventEnd, do not think that it starts immediately after domContentLoadedEventEnd.  It denotes the time when user agent set  current document readiness to "complete". In other words, it is decided by when did user agent think the the document readiness is complete and is usually close to the end of downloading all resources. 

Finally, the loadEventEnd, this denotes the time where all relative resources have been finished downloading. If the page is not fully loaded, then very likely user agent won’t set this parameter so you will find loadEventEnd is zero. For websites with a lot of objects like CNN.com, this probably gonna take long time for loadEventEnd to be setted.  

In a nutshell:

Time to finish receiving static html page:  responseEnd - navigationStart 
Time to the DOM is finished rendering and use can see something on the screen: domContentLoadedEventEnd - navigationStart
Time until all the page are fully loaded: loadEventEnd - navigationStart
