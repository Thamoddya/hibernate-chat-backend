package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import entity.Chat;
import entity.User;
import entity.User_Status;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import model.HibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

@ServerEndpoint("/HomeDataSocket")
public class HomeDataWebSocket {

    private static final Set<Session> clients = Collections.synchronizedSet(new HashSet<Session>());

    private static Gson gson = new Gson();

    @OnOpen
    public void onOpen(Session session) {
        clients.add(session);
        System.out.println("New connection: " + session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        clients.remove(session);
        System.out.println("Connection closed: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        JsonObject request = gson.fromJson(message, JsonObject.class);
        int userId = request.get("id").getAsInt();

        // Load data and broadcast it to all clients
        broadcastUserData(userId);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println("Error on connection: " + session.getId() + " -> " + throwable.getMessage());
    }

    private void broadcastUserData(int userId) {
        org.hibernate.Session hibernateSession = null;
        try {
            hibernateSession = HibernateUtil.getSessionFactory().openSession();
            User user = (User) hibernateSession.get(User.class, userId);

            Criteria criteria = hibernateSession.createCriteria(User.class);
            criteria.add(Restrictions.ne("id", user.getId()));
            List<User> otherUserList = criteria.list();

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", true);
            responseJson.add("otherusersJsonArray", loadUsers(otherUserList, user, hibernateSession));

            synchronized (clients) {
                for (Session client : clients) {
                    client.getBasicRemote().sendText(gson.toJson(responseJson));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (hibernateSession != null) {
                hibernateSession.close();
            }
        }
    }

    private JsonArray loadUsers(List<User> otherUserList, User user, org.hibernate.Session hibernateSession) {
        JsonArray jsonArray = new JsonArray();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy, MMM dd hh:mm a");

        for (User otherUser : otherUserList) {
            JsonObject jsonChatItem = new JsonObject();
            jsonChatItem.addProperty("other_user_id", otherUser.getId());
            jsonChatItem.addProperty("other_user_name", otherUser.getFirst_name() + " " + otherUser.getLast_name());
            jsonChatItem.addProperty("other_user_mobile", otherUser.getMobile());
            jsonChatItem.addProperty("other_user_status", otherUser.getUser_Status().getId());

            // Fetch the last chat message
            Criteria chatCriteria = hibernateSession.createCriteria(Chat.class);
            chatCriteria.add(
                    Restrictions.or(
                            Restrictions.and(
                                    Restrictions.eq("from_user", user),
                                    Restrictions.eq("to_user", otherUser)
                            ),
                            Restrictions.and(
                                    Restrictions.eq("from_user", otherUser),
                                    Restrictions.eq("to_user", user)
                            )
                    )
            );
            chatCriteria.addOrder(Order.desc("id"));
            chatCriteria.setMaxResults(1);

            List<Chat> chatList = chatCriteria.list();
            if (chatList.isEmpty()) {
                jsonChatItem.addProperty("message", "Say hi, to your new friend!");
                jsonChatItem.addProperty("dateTime", dateFormat.format(user.getRegister_date_time()));
                jsonChatItem.addProperty("chat_status_id", 1);
            } else {
                jsonChatItem.addProperty("message", chatList.get(0).getMessage());
                jsonChatItem.addProperty("dateTime", dateFormat.format(chatList.get(0).getDate_time()));
                jsonChatItem.addProperty("chat_status_id", chatList.get(0).getChat_Status().getId());
            }

            jsonArray.add(jsonChatItem);
        }

        return jsonArray;
    }
}
