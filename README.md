### Live Football World Cup Score Board

A simple java library implementation which shows all the ongoing matches and their scores.

### Overview
The 2026 World Cup will see **48 teams** competing.
With a nature of Football (goals per game) and a small number of parallel games
it's expected that **Write ops are Very Rare with very low concurrency (parallel games)**.

While lots of watchers (software clients in term) are expected
meaning **very high Read rate and very high concurrency**.

Thus much more preferences are given to Read ops:
_Summary_ are calculated eagerly on any Match changes.
What allows to give a live (low-latency) summary of WorldCup with 'zero' CPU computation & Memory allocation,
but with slower Write ops as a tradeoff.

API returns only immutable objects (collections and entities). 

### Soft removal of Finished matches
According to the requirements the finished matches have to be removed from the ScoreBoard 'immediately'.
But this point might be discussed with business representatives as this behavior might confuse the clients of app.
Imagine an ongoing final match of World Cup, which will suddenly disappear from our ScoreBoard right after the finish. 

To take it into account, the finished matches are still kept in ScoreBoard internally even after 'finish' 
and API behaves according to the requirements. But the implementation is open for further enhancements.

**Proposal**: keep the finish timestamp as match meta-data and show even finished matches some time longer

### Build
Gradle wrapper, Java17+ 

./gradlew build - Assembles and tests this project.


### Other ideas of implementation
It would be interesting to implement the internal storage of data similarly to how RDBMS handles it:
with a primary (clustered index) and secondary index (covered index). Then we will have our 'write' operations
going through 'primary key' (homeTeam & awayTeam) and 'secondary index' (summary query). 
The 'summary index' will be updated as now - on changes of Matches.  

It would improve the Write operations as it will be based on a Key access only. 
But this implementation will be harder to evaluate and would violate the requirements to keep it simple as much.