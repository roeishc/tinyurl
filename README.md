# Tinyurl

Spring Boot application exposing API to shorten URLs, tinyurl/bitly style.
<br><br>
<a href="https://roei-tinyurl.runmydocker-app.com/swagger-ui.html">Swagger UI</a> running on <a href="https://runmydocker.com/">runmydocker.com</a>.
<br><br>
Utilizing:
- **Redis** for quick fetching of the original URL - The shortened URL maps to the original URL.
- **MongoDB** for saving users, and "high-level" statistics of shortened URLs usage for each user, such as how many total clicks, and clicks distribution per month:<br>
  <img src="https://github.com/roeishc/tinyurl/assets/95538414/8bb70327-0122-4e53-903b-6d4b30cca68f" width=250>
- **Cassandra** for precise tracking of all clicks for all users (exact time of each click):<br>
  <img src="https://github.com/roeishc/tinyurl/assets/95538414/fb3d6aaa-6815-432a-b281-50da465219fb" width=250>


<br>
<a href="https://roei-tinyurl.runmydocker-app.com/swagger-ui.html">Swagger UI</a> available for:
<br><br>
<ul>
  <li>Adding a user.</li>
  <li>Shortening a URL.</li>
  <li>Getting/setting the original URL with the shortened version ("tiny").</li>
  <li>Getting information about a user's shortened URLs: all-time clicks (on all of the user's shortened URLs), and how many clicks were on each shortened URL in each month. See above (first image).</li>
  <li>Getting information about each click on all of the user's shortened URLs. See above (second image).</li>
  <li>Redirect (for accessing the original URL with the shortened URL).</li>
</ul>

<br><br>
To run it on your machine, run `docker-compose.yml` to have Redis, MongoDB, and Cassandra running on containers on your machine, and execute the code in `init-cassandra.cql` in the Cassandra container (via UI or CLI). Then, run the project (via IntelliJ, etc.). The Swagger UI will be available at localhost:8080/swagger-ui.html
