###OPDEFX - Automated IGM profile publication with a ZIO backend

**Installation Instructions**

1. Install scala & sbt.
2. Clone the project.
3. Edit app.properties file.
4. Open a terminal console.
4. Run sbt "set test in assembly := {}" clean assembly. This command will build a fat jar executable.

![GUI](assets/gui.PNG)

**Under the hood**

![IGM file processing](assets/under_the_hood.png)

**TODO**

- [ ] Migrate to ZIO 2.0
- [ ] Add transactional semantics to IGM publications
- [ ] Add retry semantics
- [ ] Improve GUI experience
- [ ] Enhance functionality to support more procedures provided by OPDM we services