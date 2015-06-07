---------------------------------
Providentia - A fund price oracle
---------------------------------

Scala, Java, Akka, Play, CoffeeScript mutual fund tracking financial app

Estimates mutual and index fund realtime developments based on stock ticker values
from yahoo finance API. For international stocks, takes into to account currency developments
vs. Norwegian Kroner ("the nokkie"), deduced from developments vs USD by necessity (only free currency api I could find)

Stock ticker charts and actors built on template in activator.

Fund holdings pulled from csv files in format [StockName][PercentageOfFund][][][optional: StockSymbol]

Stock symbols will be queried from company name by calls to yahoo finance, if not provided in CSV, and then
stored in Postgres db.

https://providentia-funds.herokuapp.com

License:
http://apache.org/licenses/LICENSE-2.0



