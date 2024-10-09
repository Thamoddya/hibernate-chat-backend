package controller;

import com.google.gson.Gson;
import entity.User;
import model.HibernateUtil;
import org.hibernate.Session;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "GetUserServlet", urlPatterns = {"/GetUser"})
public class GetUserServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userIdParam = req.getParameter("user_id");
        if (userIdParam == null || userIdParam.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing or invalid 'user_id' parameter");
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(userIdParam);
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid 'user_id' parameter");
            return;
        }

        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            User user = (User) session.get(User.class, userId);

            if (user == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("User not found");
                return;
            }

            Gson gson = new Gson();
            String userJson = gson.toJson(user);

            resp.setContentType("application/json");
            resp.getWriter().write(userJson);

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
