import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * <p>
 * class CCmailer reads in the required files and send an e-mail based on the
 * template in used for Computing Club's e-mail blast
 * </p>
 * <p>
 * <b>Main References:</b><br>
 * http://stackoverflow.com/questions/46663/how-can-i-send-an-email-
 * by-java-application-using-gmail-yahoo-or-hotmail<br>
 * http://www.tutorialspoint.com
 * /javamail_api/javamail_api_send_inlineimage_in_email.htm<br>
 * http://www.codejava.net/java-ee/javamail/embedding-images-into-e-mail-with-
 * javamail
 * </p>
 * 
 * @author Benedict
 *
 */
public class CCmailer {

    private static final String FILE_PRIVATE_INFO = "privateinfo.txt";

    private static HashMap<String, String> infoMap =
            new HashMap<String, String>();

    private static ArrayList<InternetAddress> to =
            new ArrayList<InternetAddress>();
    private static ArrayList<InternetAddress> cc =
            new ArrayList<InternetAddress>();
    private static ArrayList<InternetAddress> bcc =
            new ArrayList<InternetAddress>();
    private static ArrayList<InternetAddress> replyTo =
            new ArrayList<InternetAddress>();

    private static BufferedReader bufferedInput;

    private static MimeMultipart generateHtml(String path)
            throws MessagingException, IOException {
        // This mail has 2 part, the BODY and the embedded image
        MimeMultipart multipart = new MimeMultipart("related");

        // first part (the html)
        BodyPart messageBodyPart = new MimeBodyPart();

        HtmlGenerator htmlGenerator = HtmlGenerator.getInstance();
        String htmlText = htmlGenerator.generateHtml(path);

        messageBodyPart.setContent(htmlText, "text/html");
        // add it
        multipart.addBodyPart(messageBodyPart);

        try (DirectoryStream<Path> stream =
                Files.newDirectoryStream(Paths.get(path), "*.png")) {
            for (Path entry : stream) {
                String entryName = entry.getFileName().toString();
                System.out.println(entryName);
                // second part (the image)
                messageBodyPart = new MimeBodyPart();
                DataSource fds = new FileDataSource(path + entryName);

                messageBodyPart.setDataHandler(new DataHandler(fds));
                System.out.println(entryName.substring(0,
                        entryName.length() - 4) + "\n");
                messageBodyPart.setHeader("Content-ID",
                        "<" + entryName.substring(0, entryName.length() - 4)
                                + ">");

                // add image to the multipart
                multipart.addBodyPart(messageBodyPart);
            }
        }

        // put everything together
        return multipart;
    }

    private static void sendEmail() {
        Properties properties = System.getProperties();

        properties.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(properties);
        MimeMessage message = new MimeMessage(session);
        InternetAddress internetAddressArray[] = {};

        try {
            message.setFrom(new InternetAddress(infoMap.get("from"), infoMap
                    .get("from-name")));
            message.setReplyTo(replyTo.toArray(internetAddressArray));
            message.setRecipients(Message.RecipientType.TO,
                    to.toArray(internetAddressArray));
            /*
             * message.setRecipients(Message.RecipientType.CC,
             * cc.toArray(internetAddressArray));
             * message.setRecipients(Message.RecipientType.BCC,
             * bcc.toArray(internetAddressArray));
             */

            message.setSubject(infoMap.get("subject"));

            message.setContent(generateHtml("sample/"));

            HtmlGenerator htmlGenerator = HtmlGenerator.getInstance();

            // message.setText("HI");
            System.out.println("passed");
            Transport transport = session.getTransport("smtp");
            transport.connect(infoMap.get("host"), infoMap.get("username"),
                    infoMap.get("password"));
            System.out.println("cleared");
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        try {
            bufferedInput =
                    new BufferedReader(new InputStreamReader(
                            new FileInputStream(FILE_PRIVATE_INFO)));
            String line;
            int index;
            while ((line = bufferedInput.readLine()) != null) {
                index = line.indexOf(": ");
                infoMap.put(line.substring(0, index),
                        line.substring(index + 2, line.length()));
            }
            bufferedInput.close();

            to.add(new InternetAddress(infoMap.get("to"), infoMap
                    .get("to-name")));
            // cc.add(new InternetAddress(infoMap.get("cc")));
            // bcc.add(new InternetAddress(infoMap.get("bcc")));
            replyTo.add(new InternetAddress(infoMap.get("reply-to"), infoMap
                    .get("reply-to-name")));

            sendEmail();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}