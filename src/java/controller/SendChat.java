package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entity.Chat;
import entity.Chat_Status;
import entity.User;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

@WebServlet(name = "SendChat", urlPatterns = {"/SendChat"})
public class SendChat extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", false);
        Gson gson = new Gson();

        BufferedReader reader = req.getReader();
        JsonObject jsonRequest = gson.fromJson(reader, JsonObject.class);

        if (jsonRequest == null
                || !jsonRequest.has("logged_user_id")
                || !jsonRequest.has("other_user_id")
                || !jsonRequest.has("message")) {
            responseJson.addProperty("error", "Missing parameters");
            resp.setContentType("application/json");
            resp.getWriter().write(gson.toJson(responseJson));
            return;
        }

        String logged_user_id = jsonRequest.get("logged_user_id").getAsString();
        String other_user_id = jsonRequest.get("other_user_id").getAsString();
        String message = jsonRequest.get("message").getAsString();

        // Start Hibernate session
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();

        try {
            // Retrieve users
            User loggedUser = (User) session.get(User.class, Integer.valueOf(logged_user_id));
            User otherUser = (User) session.get(User.class, Integer.valueOf(other_user_id));

            if (loggedUser == null || otherUser == null) {
                responseJson.addProperty("error", "User not found");
                resp.setContentType("application/json");
                resp.getWriter().write(gson.toJson(responseJson));
                return;
            }

            // Create Chat instance
            Chat chat = new Chat();
            Chat_Status chatStatus = (Chat_Status) session.get(Chat_Status.class, 2); // Assuming 2 is "unseen"
            chat.setChat_Status(chatStatus);
            chat.setDate_time(new Date());
            chat.setFrom_user(loggedUser);
            chat.setTo_user(otherUser);
            chat.setMessage(message);

            // Save the chat message
            session.save(chat);
            tx.commit();

            responseJson.addProperty("status", true);
        } catch (NumberFormatException e) {
            responseJson.addProperty("error", "Invalid user ID format");
        } catch (ConstraintViolationException e) {
            responseJson.addProperty("error", "Data constraint violation");
        } catch (IOException e) {
        } finally {
            if (session != null) {
                session.close();
            }
        }

        // Send JSON response
        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseJson));
    }
}
