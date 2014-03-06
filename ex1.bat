@echo off
java -Djava.util.logging.SimpleFormatter.format="%%1$tH:%%1$tM:%%1$tS %%4$s %%2$s %%5$s%%6$s%%n" -Djava.security.policy="no.policy" -jar ex1.jar %*