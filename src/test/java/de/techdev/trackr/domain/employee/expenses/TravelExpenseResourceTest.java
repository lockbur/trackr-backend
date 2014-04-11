package de.techdev.trackr.domain.employee.expenses;

import de.techdev.trackr.domain.AbstractDomainResourceTest;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;

import javax.json.stream.JsonGenerator;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.function.Function;

import static de.techdev.trackr.domain.DomainResourceTestMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Moritz Schulze
 */
public class TravelExpenseResourceTest extends AbstractDomainResourceTest<TravelExpense> {

    private final Function<TravelExpense, MockHttpSession> sameEmployeeSessionProvider;
    private final Function<TravelExpense, MockHttpSession> otherEmployeeSessionProvider;

    public TravelExpenseResourceTest() {
        sameEmployeeSessionProvider = travelExpense -> employeeSession(travelExpense.getReport().getEmployee().getId());
        otherEmployeeSessionProvider = travelExpense -> employeeSession(travelExpense.getReport().getEmployee().getId() + 1);
    }

    @Override
    protected String getResourceName() {
        return "travelExpenses";
    }

    @Test
    public void rootNotExported() throws Exception {
        assertThat(root(adminSession()), isMethodNotAllowed());
    }

    @Test
    public void oneNotExported() throws Exception {
        assertThat(one(adminSession()), isMethodNotAllowed());
    }

    @Test
    public void createAllowedForSelf() throws Exception {
        assertThat(create(sameEmployeeSessionProvider), isCreated());
    }

    @Test
    @Ignore
    public void createNotAllowedForOther() throws Exception {
        assertThat(create(otherEmployeeSessionProvider), isForbidden());
    }

    @Test
    public void updateAllowedForSelf() throws Exception {
        assertThat(update(sameEmployeeSessionProvider), isUpdated());
    }

    @Test
    public void deletePendingAllowed() throws Exception {
        assertThat(remove(sameEmployeeSessionProvider), isNoContent());
    }

    @Test
    public void deleteAcceptedNotAllowed() throws Exception {
        TravelExpense travelExpense = dataOnDemand.getRandomObject();
        travelExpense.getReport().setStatus(TravelExpenseReportStatus.ACCEPTED);
        repository.save(travelExpense);
        assertThat(removeUrl(employeeSession(travelExpense.getReport().getId()), "/travelExpenses/" + travelExpense.getId()), isForbidden());
    }

    @Test
    @Ignore
    public void changeReportNotAllowed() throws Exception {
        assertThat(updateLink(supervisorSession(), "report", "/travelExpenseReports/0"), isForbidden());
    }

    @Override
    protected String getJsonRepresentation(TravelExpense travelExpense) {
        StringWriter writer = new StringWriter();
        JsonGenerator jg = jsonGeneratorFactory.createGenerator(writer);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        jg.writeStartObject()
          .write("cost", travelExpense.getCost())
          .write("vat", travelExpense.getVat())
          .write("fromDate", sdf.format(travelExpense.getFromDate()))
          .write("toDate", sdf.format(travelExpense.getToDate()))
          .write("submissionDate", sdf2.format(travelExpense.getSubmissionDate()))
          .write("type", travelExpense.getType().toString())
          .write("report", "/travelExpenseReports/" + travelExpense.getReport().getId());
        if (travelExpense.getId() != null) {
            jg.write("id", travelExpense.getId());
        }
        jg.writeEnd().close();
        return writer.toString();
    }
}
