package de.hype.bbsentials.client.common.client.objects;

import de.hype.bbsentials.client.common.client.BBsentials;

public class ServerSwitchTask {
    public static int counter = 0;
    private final int id = counter++;
    public Runnable runnable;
    public boolean permanent;

    private ServerSwitchTask(Runnable runnable) {
        this.runnable = runnable;
    }

    private ServerSwitchTask(Runnable runnable, boolean permanent) {
        this.runnable = runnable;
        this.permanent = permanent;
    }

    public static int onServerLeaveTask(Runnable runnable, boolean permanent){
        ServerSwitchTask task = new ServerSwitchTask(runnable,permanent);
        BBsentials.onServerLeave.put(task.id,task);
        return task.id;
    }
    public static int onServerLeaveTask(Runnable runnable){
        ServerSwitchTask task = new ServerSwitchTask(runnable);
        BBsentials.onServerLeave.put(task.id,task);
        return task.id;
    }
    public static int onServerJoinTask(Runnable runnable, boolean permanent){
        ServerSwitchTask task = new ServerSwitchTask(runnable,permanent);
        BBsentials.onServerJoin.put(task.id,task);
        return task.id;
    }
    public static int onServerJoinTask(Runnable runnable){
        ServerSwitchTask task = new ServerSwitchTask(runnable);
        BBsentials.onServerJoin.put(task.id,task);
        return task.id;
    }
    public int getId() {
        return id;
    }

    public void run() {
        runnable.run();
    }
}