package com.disney.cast.platform.estp.api.alert.get;

import static com.disney.automation.servicetesting.config.Framework.CONFIGURATION;
import static com.disney.cast.platform.vacationplanner.api.app.AlertApi.getAlert;
import static com.disney.cast.platform.vacationplanner.data.DataManager.ALERT_DATA_MANAGER;
import static com.disney.cast.platform.vacationplanner.test.api.ApiAuthLevel.LEADER;
import static com.disney.cast.platform.vacationplanner.test.api.ApiAuthLevel.PLANNER;
import static com.disney.cast.platform.vacationplanner.test.api.ApiAuthLevel.SNOWADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.disney.automation.servicetesting.core.ApiTestResponse;
import com.disney.cast.platform.common.api.model.Result;
import com.disney.cast.platform.common.api.snow.tables.model.SysUserTableRecord;
import com.disney.cast.platform.vacationplanner.api.app.model.Alert;
import com.disney.cast.platform.vacationplanner.api.snow.tables.AlertTableApi;
import com.disney.cast.platform.vacationplanner.api.snow.tables.model.AlertTableRecord;
import com.disney.cast.platform.vacationplanner.api.snow.tables.model.PayrollTableRecord;
import com.disney.cast.platform.vacationplanner.data.DataManager;
import com.disney.cast.platform.vacationplanner.test.api.AbstractVacationPlannerRewardsApiTest;
import com.fasterxml.jackson.core.type.TypeReference;

public class GetAlertTest extends AbstractVacationPlannerRewardsApiTest {

    private static final int ACTIVE_ALERT_TO_ADD = 1;
    private static final int INACTIVE_ALERT_TO_ADD = 1;

    public GetAlertTest() throws MalformedURLException {
    }

    @Override
    public void specificSetUp() throws Exception {
        ALERT_DATA_MANAGER.addActive(ACTIVE_ALERT_TO_ADD);
        ALERT_DATA_MANAGER.addInactive(INACTIVE_ALERT_TO_ADD);
        SysUserTableRecord planner = DataManager.USER_DATA_MANAGER
                .getUserByName(CONFIGURATION.account(PLANNER.toString()).getUserName());
        PayrollTableRecord payroll = DataManager.PAYROLL_DATA_MANAGER.addPayroll(1).iterator().next();
        DataManager.UI_USER_DATA_MANAGER
                .addActive(planner,
                        DataManager.USER_DATA_MANAGER
                                .getUserByName(CONFIGURATION.account(LEADER.toString()).getUserName()),
                        payroll);
    }

    @Test
    public void getAlertTest() throws Exception {
        AlertTableApi alertTableApi = new AlertTableApi();
        List<AlertTableRecord> activeAlertsRecordsFromTableApi = alertTableApi
                .get(clients().get(SNOWADMIN.toString()),
                        "?sysparm_query=u_active%3Dtrue")
                .getBodyObject(new TypeReference<Result<List<AlertTableRecord>>>() {
                })
                .getResult();

        List<AlertTableRecord> inactiveAlertsRecordsFromTableApi = alertTableApi
                .get(clients().get(SNOWADMIN.toString()),
                        "?sysparm_query=u_active%3Dfalse")
                .getBodyObject(new TypeReference<Result<List<AlertTableRecord>>>() {
                })
                .getResult();

        ApiTestResponse getAlertResponse = getAlert(clients().get(PLANNER.toString()));
        int getAlertResponseStatusCode = getAlertResponse.getStatus();
        List<Alert> returnedAlerts = getAlertResponse
                .getBodyObject(new TypeReference<Result<List<Alert>>>() {
                })
                .getResult();
        assertEquals(
                String.format(
                        "When making a GET request to /alert, the HTTP status code of the response should be 200 instead of %s",
                        getAlertResponse.getStatus()),
                200, getAlertResponseStatusCode);

        assertEquals(
                String.format(
                        "The amount of returned Alerts expected is %s when the actual amount of active alerts is %s",
                        returnedAlerts.size(), activeAlertsRecordsFromTableApi.size()),
                activeAlertsRecordsFromTableApi.size(), returnedAlerts.size());

        List<AlertVO> apiTableAlerts = activeAlertsRecordsFromTableApi
                .stream()
                .map(alert -> new AlertVO(alert.getUType(), alert.getUBody()))
                .collect(Collectors.toList());

        List<AlertVO> apiTableInactiveAlerts = inactiveAlertsRecordsFromTableApi
                .stream()
                .map(alert -> new AlertVO(alert == null ? "" : alert.getUType(),
                        alert == null ? "" : alert.getUBody()))
                .collect(Collectors.toList());

        List<AlertVO> apiAlerts = returnedAlerts
                .stream()
                .map(alert -> new AlertVO(alert.getType(), alert.getBody()))
                .collect(Collectors.toList());

        assertTrue(String.format("The Alerts don't match. \nExpected: %s\nActual: %s", apiTableAlerts, apiAlerts),
                apiTableAlerts.containsAll(apiAlerts) && apiAlerts.containsAll(apiTableAlerts));

        assertFalse(String.format(
                "The Alerts from api should not match with alerts from table api. \nExpected: %s\nActual: %s",
                apiTableInactiveAlerts, apiAlerts),
                apiTableInactiveAlerts.containsAll(apiAlerts) && apiAlerts.containsAll(apiTableInactiveAlerts));
    }

    public static class AlertVO {
        final String type;
        final String body;

        public AlertVO(String type, String body) {
            this.type = type;
            this.body = body;
        }

        @Override
        public boolean equals(Object o) {
            AlertVO a = (AlertVO) o;
            return a.type.equals(type) && a.body.equals(body);
        }

        @Override
        public String toString() {
            return String.format("Body: '%s'\tType: %s", body, type);
        }
    }
}