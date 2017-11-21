package ren.helloworld.upload2pgyer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hudson.model.Run;
import hudson.model.TaskListener;
import ren.helloworld.upload2pgyer.java.Message;
import ren.helloworld.upload2pgyer.java.PgyerBean;
import ren.helloworld.upload2pgyer.java.UploadBean;
import ren.helloworld.upload2pgyer.java.UploadPgyer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

public class UploadHelper {
    private static final String TAG = "[UPLOAD TO PGYER] - ";

    /**
     * @param run        build
     * @param listener   listener
     * @param uploadBean uploadBean
     * @return success or failure
     * @throws IOException          IOException
     * @throws InterruptedException InterruptedException
     */
    public static boolean upload(@Nonnull Run<?, ?> run, @Nonnull final TaskListener listener, UploadBean uploadBean) throws IOException, InterruptedException {
        Message message = new Message() {
            @Override
            public void message(boolean needTag, String mesage) {
                listener.getLogger().println((needTag ? TAG : "") + mesage);
            }
        };

        // expand params
        uploadBean.setUkey(run.getEnvironment(listener).expand(uploadBean.getUkey()));
        uploadBean.setApiKey(run.getEnvironment(listener).expand(uploadBean.getApiKey()));
        uploadBean.setScandir(run.getEnvironment(listener).expand(uploadBean.getScandir()));
        uploadBean.setWildcard(run.getEnvironment(listener).expand(uploadBean.getWildcard()));
        uploadBean.setInstallType(run.getEnvironment(listener).expand(uploadBean.getInstallType()));
        uploadBean.setPassword(run.getEnvironment(listener).expand(uploadBean.getPassword()));
        uploadBean.setUpdateDescription(run.getEnvironment(listener).expand(uploadBean.getUpdateDescription()));
        uploadBean.setQrcodePath(run.getEnvironment(listener).expand(uploadBean.getQrcodePath()));
        uploadBean.setEnvVarsPath(run.getEnvironment(listener).expand(uploadBean.getEnvVarsPath()));

        // upload
        PgyerBean pgyerBean = UploadPgyer.upload2Pgyer(true, uploadBean, message);
        if (pgyerBean == null) return false;

        // http://jenkins-ci.361315.n4.nabble.com/Setting-an-env-var-from-a-build-step-td4657347.html
        message.message(true, "now setting the envs……");
        String data = new Gson().toJson(pgyerBean.getData());
        Map<String, String> maps = new Gson().fromJson(data, new TypeToken<Map<String, String>>() {
        }.getType());
        for (Map.Entry<String, String> entry : maps.entrySet()) {
            String key = entry.getKey();
            if (key.equals("userKey")) continue;
            run.addAction(new PublishEnvVarAction(key, entry.getValue()));
            message.message(true, "The ${" + key + "} set up successfully! now you can use it anywhere!");
        }
        message.message(true, "congratulations!\n");
        return true;
    }
}
