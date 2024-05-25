/**
 * Api тесты для Rest Assured
 * Тест 1
 * 1.Используя сервис https://reqres.in/ получить список пользователей со второй страницы
 * 2.Убедиться что имена файлов-аватаров пользователей совпадают;
 * 3.Убедиться, что email пользователей имеет окончание reqres.in;
 * Тест 2
 * 1.Используя сервис https://reqres.in/ протестировать регистрацию пользователя в системе
 * 2. Необходимо создание 2 тестов:
 * - успешная регистрация
 * - регистрация с ошибкой из-за отсутствия пароля,
 * 3.Проверить коды ошибок.
 * Тест 3
 * 1.Используя сервис https://reqres.in/ убедиться, что операция LIST<RESOURCE> возвращает
 * данные, отсортированные по годам.
 * Тест 4
 * 1.Используя сервис https://reqres.in/ попробовать удалить второго пользователя и сравнить
 * статус-код
 * Тест 5
 * 1.Используя сервис https://reqres.in/ обновить информацию о пользователе и сравнить дату
 * обновления с текущей датой на машине
 */

package RestAssured;

import RestAssured.api.GetAccountBalance.AccountBalanceResponse;
import RestAssured.api.PostAuthorizations.AuthRequest;
import RestAssured.api.response.*;
import RestAssured.helper.Specification;
import RestAssured.api.request.Register;
import RestAssured.api.request.UserTime;
import io.qameta.allure.junit5.AllureJunit5;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static RestAssured.api.GetAccountBalance.AccountBalanceResponse.accountBalanceResponse;
import static RestAssured.helper.ConfigProvider.*;
import static RestAssured.helper.Specification.responseSpec;
import static io.restassured.RestAssured.given;
import org.junit.jupiter.api.Test;

@Tag("API")
@ExtendWith(AllureJunit5.class)
public class ReqresInTest {

    public static String bearerToken = AuthRequest.getToken();


    @Test
    @DisplayName("Получение баланса")
    public void accountBalanceTest() {
        Specification.installSpecRequest(Specification.requestSpecificationAuth(URLVPLUSE,bearerToken));
        Specification.installSpecResponse(responseSpec(200)); // Спецификация на response
        AccountBalanceResponse balance = accountBalanceResponse();
        Assertions.assertEquals(0, balance.getBalance());
    }


    @Test
    public void checkAvatarAndIdTest(){
        Specification.installSpec(Specification.requestSpecification(URLREQRES), responseSpec(200));
        List<UserDate> users = given()
                .when()
                .get("api/users?page=2")
                .then().log().all()
                .extract().body().jsonPath().getList("data", UserDate.class);

        List<String> avatars = users.stream().map(UserDate::getAvatar).collect(Collectors.toList());
        List<String> ids = users.stream().map(x->x.getId().toString()).collect(Collectors.toList());
        for (int i = 0; i < avatars.size(); i++) {
            Assertions.assertTrue(avatars.get(i).contains(ids.get(i)));
        }
    }


    @DisplayName("Тест 2")
    @ParameterizedTest
    @CsvSource({                         // Parameterized test
            "eve.holt@reqres.in,pistol", // pass
            "eve.holt@reqre1.in,pisto3", // fail
    })
    public void successRegTest(String email, String password){

        Specification.installSpec(Specification.requestSpecification(URLREQRES), responseSpec(200));
        Integer id = 4;
        String token = "QpwL5tke4Pnpja7X4";
        Register user = new Register(email, password);
        SuccessReg successReg = given()
                .body(user)
                .when()
                .post("api/register")
                .then().log().all()
                .extract().as(SuccessReg.class);

        Assertions.assertNotNull(successReg.getId());
        Assertions.assertNotNull(successReg.getToken());

        Assertions.assertEquals(id, successReg.getId());
        Assertions.assertEquals(token, successReg.getToken());
    }


    @Test
    @DisplayName("Тест 3")
    public void unSuccessRegTest() {
        Specification.installSpec(Specification.requestSpecification(URLREQRES), responseSpec(400));
        Register user = new Register("eve.holt@reqres.in", "");
        UnSuccessReg unSuccessReg = given()
                .body(user)
                .when()
                .post("api/register")
                .then().log().all()
                .extract().as(UnSuccessReg.class);

        Assertions.assertNotNull(unSuccessReg.getError());

        Assertions.assertEquals("Missing password", unSuccessReg.getError());
    }


    @Test
    @DisplayName("Тест 4")
    public void sortedYearsTest() {
        Specification.installSpec(Specification.requestSpecification(URLREQRES), responseSpec(200));

        List<ColorsData> colors = given()
                .when()
                .get("api/unknown")
                .then().log().all()
                .extract().body().jsonPath().getList("data", ColorsData.class);
        List<Integer> years = colors.stream().map(ColorsData::getYear).collect(Collectors.toList());
        List<Integer> sortedYears = years.stream().sorted().collect(Collectors.toList());

        Assertions.assertEquals(sortedYears, years);
    }


    @Test
    @DisplayName("Тест 5")
    public void deleteUserTest() {
        Specification.installSpec(Specification.requestSpecification(URLREQRES), responseSpec(204));

        given()
                .when()
                .delete("/api/users/2")
                .then().log().all();
    }


    @Test
    @DisplayName("Тест 6")
    public void timeTest() {
        Specification.installSpec(Specification.requestSpecification(URLREQRES), responseSpec(200));
        UserTime user = new UserTime("morpheus", "zion resident");
        ServerTime time = given()
                .body(user)
                .when()
                .put("api/users/2")
                .then().log().all()
                .extract().as(ServerTime.class);

        String regexServer = "(.{6})$";
        String regexClient = "(.{12})$";
        String now = Instant.now().toString().replaceAll(regexClient, "");
        Assertions.assertEquals(now, time.getUpdatedAt().replaceAll(regexServer, ""));
    }

}