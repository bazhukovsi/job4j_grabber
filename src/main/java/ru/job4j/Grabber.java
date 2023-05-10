package ru.job4j;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.grabber.*;
import ru.job4j.grabber.model.Post;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Grabber implements Grab {
    private final Parse parse;
    private final Store store;
    private final Scheduler scheduler;
    private final int time;
    private static int count = 0;
    private static Properties config;

    public Grabber(Parse parse, Store store, Scheduler scheduler, int time) {
        this.parse = parse;
        this.store = store;
        this.scheduler = scheduler;
        this.time = time;
    }

    @Override
    public void init() throws SchedulerException, InterruptedException {
        JobDataMap data = new JobDataMap();
        data.put("store", store);
        data.put("parse", parse);
        JobDetail job = newJob(GrabJob.class)
                .usingJobData(data)
                .build();
        SimpleScheduleBuilder times = simpleSchedule()
                .withIntervalInSeconds(time)
                .repeatForever();
        Trigger trigger = newTrigger()
                .startNow()
                .withSchedule(times)
                .build();
        scheduler.scheduleJob(job, trigger);
    }

    public static class GrabJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            JobDataMap map = context.getJobDetail().getJobDataMap();
            Store store = (Store) map.get("store");
            Parse parse = (Parse) map.get("parse");
            List<Post> list;
            try {
                list = parse.list("https://career.habr.com/vacancies/java_developer?page=");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            list.forEach(store::save);
            count++;
            System.out.println("Grab Job : " + count);

        }
    }

    public void web(Store store) {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(Integer.parseInt(config.getProperty("port")))) {
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    try (OutputStream out = socket.getOutputStream()) {
                        out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
                        for (Post post : store.getAll()) {
                            out.write(post.toString().getBytes(Charset.forName("Windows-1251")));
                            out.write(System.lineSeparator().getBytes());
                        }
                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void loadCfg() throws IOException {
        config = new Properties();
        try (InputStream in = Grabber.class.getClassLoader()
                .getResourceAsStream("rabbit.properties")) {
            config.load(in);
        }
    }

    public static void main(String[] args) throws SchedulerException, IOException, InterruptedException {
        loadCfg();
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        Parse parse = new HabrCareerParse(new HabrCareerDateTimeParser());
        Store store = new PsqlStore(config);
        int time = Integer.parseInt(config.getProperty("time"));
        Grabber grab = new Grabber(parse, store, scheduler, time);
        grab.init();
        grab.web(store);
    }
}
