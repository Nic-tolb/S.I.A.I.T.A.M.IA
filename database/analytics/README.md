# Analytics queries

One SQL query per file, named after its indicator. Each file is used for:

- the Spring Boot repository (native query),
- the data analysis report,
- manual verification of the dashboard (tests 4 and 5).

| File | Indicator |
|---|---|
| `total_by_period.sql` | Total incidents per period |
| `by_category.sql` | Frequency and percentage per category |
| `weekly_trend.sql` | Daily or weekly evolution |
| `resolution_rate.sql` | Resolved over total |
| `avg_resolution_time.sql` | Average resolution time per category |
| `top_category.sql` | Most recurrent category |
| `by_priority.sql` | Distribution by priority |
