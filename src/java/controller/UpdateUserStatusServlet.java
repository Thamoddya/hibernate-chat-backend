import entity.User;
import entity.User_Status;
import org.hibernate.Session;
import org.hibernate.Transaction;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.HibernateUtil;

@WebServlet(name = "UpdateUserStatusServlet", urlPatterns = {"/updateUserStatus"})
public class UpdateUserStatusServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Set the response content type to JSON
        response.setContentType("application/json");
        Gson gson = new Gson();

        // Parse JSON data from request body using Gson
        JsonObject jsonObject = JsonParser.parseReader(request.getReader()).getAsJsonObject();
        int userId = jsonObject.get("userId").getAsInt();
        int statusId = jsonObject.get("statusId").getAsInt();

        // Validate parameters
        if (statusId != 1 && statusId != 2) {
            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("error", "Invalid status ID. Only 1 or 2 is allowed.");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        // Hibernate session
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            transaction = session.beginTransaction();

            // Retrieve User entity
            User user = (User) session.get(User.class, userId);
            if (user == null) {
                JsonObject jsonResponse = new JsonObject();
                jsonResponse.addProperty("error", "User not found.");
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

            // Retrieve the User_Status entity based on statusId
            User_Status userStatus = (User_Status) session.get(User_Status.class, statusId);
            if (userStatus == null) {
                JsonObject jsonResponse = new JsonObject();
                jsonResponse.addProperty("error", "User Status not found.");
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

            // Set the user status
            user.setUser_Status(userStatus);

            // Update the user in the database
            session.update(user);

            transaction.commit();

            // Prepare success response in JSON
            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("message", "User status updated successfully.");
            jsonResponse.addProperty("userId", user.getId());
            jsonResponse.addProperty("statusId", userStatus.getId());
            response.getWriter().write(gson.toJson(jsonResponse));
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }

            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("error", "An error occurred while updating user status: " + e.getMessage());
            response.getWriter().write(gson.toJson(jsonResponse));
            e.printStackTrace();
        } finally {
            session.close();
        }
    }
}
