package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import entity.Chat;
import entity.Chat_Status;
import entity.User;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "LoadChat", urlPatterns = {"/LoadChat"})
public class LoadChat extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = req.getReader().readLine()) != null) {
            sb.append(line);
        }

        JsonObject jsonObject = new Gson().fromJson(sb.toString(), JsonObject.class);
        int userId = jsonObject.get("user_id").getAsInt();
        int otherUserId = jsonObject.get("other_user_id").getAsInt();

        processChatRequest(userId, otherUserId, resp);
    }

    private void processChatRequest(int userId, int otherUserId, HttpServletResponse resp) throws IOException {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            Gson gson = new Gson();

            User loged_user = (User) session.get(User.class, userId);
            User other_user = (User) session.get(User.class, otherUserId);

            Criteria criteria1 = session.createCriteria(Chat.class);
            criteria1.add(Restrictions.or(
                    Restrictions.and(Restrictions.eq("from_user", loged_user), Restrictions.eq("to_user", other_user)),
                    Restrictions.and(Restrictions.eq("to_user", loged_user), Restrictions.eq("from_user", other_user))
            ));
            criteria1.addOrder(Order.asc("date_time"));

            JsonArray chatArray = new JsonArray();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, hh:mm a");
            Chat_Status chat_Status = (Chat_Status) session.get(Chat_Status.class, 1);

            List<Chat> chat_List = criteria1.list();
            for (Chat chat : chat_List) {
                JsonObject chatObject = new JsonObject();
                chatObject.addProperty("message", chat.getMessage());
                chatObject.addProperty("datetime", dateFormat.format(chat.getDate_time()));

                if (chat.getFrom_user().getId() == other_user.getId()) {
                    chatObject.addProperty("side", "left");
                    if (chat.getChat_Status().getId() == 2) {
                        chat.setChat_Status(chat_Status);
                        session.update(chat);
                    }
                } else {
                    chatObject.addProperty("side", "right");
                    chatObject.addProperty("status", chat.getChat_Status().getId());
                }
                chatArray.add(chatObject);
            }

            tx.commit();
            resp.setContentType("application/json");
            resp.getWriter().write(gson.toJson(chatArray));

        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

}
