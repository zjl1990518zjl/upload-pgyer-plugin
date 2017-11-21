package ren.helloworld.upload2pgyer;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import ren.helloworld.upload2pgyer.java.UploadBean;

/**
 * upload to jenkins
 *
 * @author myroid
 */
public class UploadBuilder extends Builder implements SimpleBuildStep {

    private String uKey;
    private String apiKey;
    private String scanDir;
    private String wildcard;
    private String installType;
    private String password;
    private String updateDescription;

    private String qrcodePath;
    private String envVarsPath;

    @DataBoundConstructor
    public UploadBuilder(String uKey, String apiKey, String scanDir, String wildcard, String installType, String password, String updateDescription, String qrcodePath, String envVarsPath) {
        this.uKey = uKey;
        this.apiKey = apiKey;
        this.scanDir = scanDir;
        this.wildcard = wildcard;
        this.installType = installType;
        this.password = password;
        this.updateDescription = updateDescription;
        this.qrcodePath = qrcodePath;
        this.envVarsPath = envVarsPath;
    }

    public String getuKey() {
        return uKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getScanDir() {
        return scanDir;
    }

    public String getWildcard() {
        return wildcard;
    }

    public String getInstallType() {
        return installType;
    }

    public String getPassword() {
        return password;
    }

    public String getUpdateDescription() {
        return updateDescription;
    }

    public String getQrcodePath() {
        return qrcodePath;
    }

    public String getEnvVarsPath() {
        return envVarsPath;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        UploadBean uploadBean = new UploadBean();
        uploadBean.setUkey(uKey);
        uploadBean.setApiKey(apiKey);
        uploadBean.setScandir(scanDir);
        uploadBean.setWildcard(wildcard);
        uploadBean.setInstallType(installType);
        uploadBean.setPassword(password);
        uploadBean.setUpdateDescription(updateDescription);
        uploadBean.setQrcodePath(qrcodePath);
        uploadBean.setEnvVarsPath(envVarsPath);
        UploadHelper.upload(run, taskListener, uploadBean);
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }


    @Symbol("upload-pgyer")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckUKey(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a uKey");
            if (!value.matches("[A-Za-z0-9]{32}"))
                return FormValidation.warning("Is this correct?");
            return FormValidation.ok();
        }

        public FormValidation doCheckApiKey(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a api_key");
            if (!value.matches("[A-Za-z0-9]{32}"))
                return FormValidation.warning("Is this correct?");
            return FormValidation.ok();
        }

        public FormValidation doCheckScanDir(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set upload ipa or apk file base dir name");
            return FormValidation.ok();
        }

        public FormValidation doCheckWildcard(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set upload ipa or apk file wildcard");
            return FormValidation.ok();
        }

        public FormValidation doCheckInstallType(@QueryParameter int value)
                throws IOException, ServletException {
            if (value < 1 || value > 3)
                return FormValidation.error("application installation, the value is (1,2,3).");
            return FormValidation.ok();
        }

        public FormValidation doCheckPassword(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a password");
            if (value.length() < 6)
                return FormValidation.warning("Isn't the password too short?");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Upload to pgyer";
        }
    }
}

