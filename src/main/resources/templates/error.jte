@param String message

<!DOCTYPE html>

<html xmlns:th="https://www.thymeleaf.org" th:replace="~{fragments/layout :: layout (~{::body},'error')}">

  <body>
    <img src="../static/resources/images/pets.png" th:src="@{/resources/images/pets.png}"/>
    <h2>Something happened...</h2>
    <p>${message}</p>
  </body>

</html>
