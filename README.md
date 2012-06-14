Guardian Polls
==============

This application simple stores a set of question to answer counts, and is able to return the appropriate set for
a given poll id.

It has only 2 main methods,

GET /:KeyId/:PollId -> returns a json representation of a poll

POST /:KeyId/:PollId -> Update the poll counts with question and answer ids

Getting it up and running
-------------------------

Because of some weird requirement ins sbt-appengine you'll need to do as https://github.com/sbt/sbt-appengine says and

```bash
export APPENGINE_SDK_HOME=<Appengine Java SDK Path>
```

You can get Appengine from https://developers.google.com/appengine/downloads.html#Google_App_Engine_SDK_for_Java

```bash
sbt
[...]
package
[...]
appengine-dev-server --port=8090
```

If you are running the Guardian CMS locally, you'll need to change your environment.DEV.properties to update
async.polls.host to http://localhost:8090/DEV

Importing Data
--------------

If you want guardian entries for polls for hostorical polls, you'll need to run the /KEY/data/import url and upload
a CSV file.  The file is formatted like
````
332038060,64,947,67,31
````
Where we have
````
PollId,QuestionId,QuestionTotal,AnswerId,AnswerCount
````

Scalability
-----------

In defiance of normal appengine wisdom it does not use sharded counters.  It probably should do, but it'd need to
fail to scale and I reckon it can do about 2 requests a second without, which I suspect we won't hit.