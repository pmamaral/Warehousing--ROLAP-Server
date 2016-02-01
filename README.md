# Warehousing--ROLAP-Server
Java Web OLAP Tool - ROLAP Server

Integrates with fact table based models instantiated in relational databases. 
Each database must be mapped through a XML meta model definition.
Exposes data filtering/aggregation operations like slice, filter before, filter after, drill-down and drill-up, and data visualization and management operations that allow multidimensional queries to be answered and the storing of prepared queries (reports).

Handles typical fact tables and dimensions (except spatial and temporal dimensions). Also supports snowflaked dimensions (dimensions distributed among 1:N relations in the database).
