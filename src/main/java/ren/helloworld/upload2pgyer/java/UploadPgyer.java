package ren.helloworld.upload2pgyer.java;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.tools.ant.DirectoryScanner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dafan on 2017/5/4 0004.
 */
public class UploadPgyer {
	private static final String TAG = "[UPLOAD TO PGYER] - ";
	private static final String UPLOAD_URL = "https://qiniu-storage.pgyer.com/apiv1/app/upload";

	public static void main(String[] args) {

		Message listener = new Message() {
			@Override
			public void message(boolean needTag, String mesage) {
				System.out.println((needTag ? TAG : "") + mesage);
			}
		};

		printHeaderInfo(listener);
		UploadBean uploadBean = parseArgs(args, listener);
		if (uploadBean == null) return;
		upload2Pgyer(false, uploadBean, listener);
	}

	/**
	 * 解析参数
	 *
	 * @param args
	 * @param listener
	 * @return
	 */
	private static UploadBean parseArgs(String[] args, Message listener) {
		// check args length
		int length = args.length;
		if (length == 0 || length % 2 != 0) {
			printMessage(listener, true, "args length is error!\n");
			return null;
		}

		// args to map
		Map<String, String> maps = new HashMap<>();
		for (int i = 0; i < args.length; i += 2) {
			maps.put(args[i], args[i + 1]);
		}

		// check uKey
		if (!maps.containsKey("-uKey")) {
			printMessage(listener, true, "uKey not found!\n");
			return null;
		}
		// check apiKey
		if (!maps.containsKey("-apiKey")) {
			printMessage(listener, true, "apiKey not found!\n");
			return null;
		}
		// check scanDir
		if (!maps.containsKey("-scanDir")) {
			printMessage(listener, true, "scanDir not found!\n");
			return null;
		}
		// check scanFile
		if (!maps.containsKey("-wildcard")) {
			printMessage(listener, true, "wildcard not found!\n");
			return null;
		}

		// params to uploadBean
		UploadBean uploadBean = new UploadBean();
		uploadBean.setUkey(maps.get("-uKey"));
		uploadBean.setApiKey(maps.get("-apiKey"));
		uploadBean.setScandir(maps.get("-scanDir"));
		uploadBean.setWildcard(maps.get("-wildcard"));
		uploadBean.setPassword(maps.containsKey("-password") ? maps.get("-password") : "");
		uploadBean.setQrcodePath(maps.containsKey("-qrcodePath") ? maps.get("-qrcodePath") : null);
		uploadBean.setEnvVarsPath(maps.containsKey("-envVarsPath") ? maps.get("-envVarsPath") : null);
		uploadBean.setInstallType(maps.containsKey("-installType") ? maps.get("-installType") : "1");
		uploadBean.setUpdateDescription(maps.containsKey("-updateDescription") ? maps.get("-updateDescription") : "");
		return uploadBean;
	}

	/**
	 * @param uploadBean
	 * @param listener
	 */
	public static PgyerBean upload2Pgyer(boolean printHeader, UploadBean uploadBean, final Message listener) {
		// print header info
		if (printHeader) printHeaderInfo(listener);

		// find upload file
		uploadBean.setUploadFile(findFile(uploadBean, listener));

		// check upload file
		if (uploadBean.getUploadFile() == null) {
			printMessage(listener, true, "upload file not found，plase check scandir or wildcard!\n");
			return null;
		}
		File uploadFile = new File(uploadBean.getUploadFile());
		if (!uploadFile.exists() || !uploadFile.isFile()) {
			printMessage(listener, true, "upload file not found，plase check scandir or wildcard!\n");
			return null;
		}

		String result = "";
		FileInputStream fis = null;
		try {
			printMessage(listener, true, "upload：" + uploadFile.getName() + " to " + UPLOAD_URL);
			fis = new FileInputStream(uploadFile);
			Document document = Jsoup.connect(UPLOAD_URL)
					.ignoreContentType(true)
					.data("uKey", uploadBean.getUkey())
					.data("_api_key", uploadBean.getApiKey())
					.data("installType", uploadBean.getInstallType())
					.data("password", uploadBean.getPassword())
					.data("updateDescription", uploadBean.getUpdateDescription())
					.data("file", uploadFile.getName(), fis)
					.post();
			result = document.body().text();

			PgyerBean pgyerBean = new Gson().fromJson(result, new TypeToken<PgyerBean>() {
			}.getType());

			if (pgyerBean.getCode() != 0) {
				printMessage(listener, true, "upload to pgyer failure!");
				printMessage(listener, true, "error code：" + pgyerBean.getCode());
				printMessage(listener, true, "error message：" + pgyerBean.getMessage() + "\n");
				return null;
			}

			pgyerBean.getData().setAppPgyerURL("https://www.pgyer.com/" + pgyerBean.getData().getAppShortcutUrl());
			pgyerBean.getData().setAppBuildURL("https://www.pgyer.com/" + pgyerBean.getData().getAppKey());

			printMessage(listener, true, "upload to pgyer success!\n");
			printResultInfo(pgyerBean, listener);
			writeEnvVars(uploadBean, pgyerBean, listener);
			downloadQrcode(uploadBean, pgyerBean, listener);

			return pgyerBean;
		} catch (Exception e) {
			listener.message(true, "pgyer result: " + result);
			listener.message(true, "ERROR: " + e.getMessage() + "\n");
			return null;
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * find file
	 *
	 * @param bean
	 * @param listener
	 * @return
	 */
	private static String findFile(final UploadBean bean, Message listener) {
		File dir = new File(bean.getScandir());
		if (dir == null || !dir.exists() || !dir.isDirectory()) {
			printMessage(listener, true, "scan dir:" + dir.getAbsolutePath());
			printMessage(listener, true, "scan dir isn't exist or it's not a directory!");
			return null;
		}

		DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir(bean.getScandir());
		scanner.setIncludes(new String[]{bean.getWildcard()});
		scanner.setCaseSensitive(true);
		scanner.scan();
		String[] uploadFiles = scanner.getIncludedFiles();

		if (uploadFiles == null || uploadFiles.length == 0)
			return null;
		if (uploadFiles.length == 1)
			return new File(dir, uploadFiles[0]).getAbsolutePath();

		List<String> strings = Arrays.asList(uploadFiles);
		Collections.sort(strings, new FileComparator(dir));
		String uploadFiltPath = new File(dir, strings.get(0)).getAbsolutePath();
		printMessage(listener, true, "Found " + uploadFiles.length + " files, the default choice of the latest modified file!");
		printMessage(listener, true, "The latest modified file is " + uploadFiltPath + "\n");
		return uploadFiltPath;
	}

	/**
	 * download qrcode
	 *
	 * @param uploadBean
	 * @param pgyerBean
	 * @param listener
	 */
	private static void downloadQrcode(UploadBean uploadBean, PgyerBean pgyerBean, Message listener) {
		if (uploadBean.getQrcodePath() == null) return;
		if (replaceBlank(uploadBean.getQrcodePath()).length() == 0) return;
		printMessage(listener, true, "now download qrcode");
		File qrcode = new File(uploadBean.getQrcodePath());
		if (!qrcode.getParentFile().exists() && !qrcode.getParentFile().mkdirs()) {
			printMessage(listener, true, "orz..., download qrcode failure……" + "\n");
			return;
		}
		File file = download(pgyerBean.getData().getAppQRCodeURL(), qrcode.getParentFile().getAbsolutePath(), qrcode.getName());
		if (file != null) printMessage(listener, true, "download qrcode success! " + file + "\n");
		else printMessage(listener, true, "orz..., download qrcode failure……" + "\n");
	}

	/**
	 * write env vars to file
	 *
	 * @param uploadBean
	 * @param pgyerBean
	 * @param listener
	 */
	private static void writeEnvVars(UploadBean uploadBean, PgyerBean pgyerBean, Message listener) {
		if (uploadBean.getEnvVarsPath() == null) return;
		if (replaceBlank(uploadBean.getEnvVarsPath()).length() == 0) return;
		printMessage(listener, true, "now write env vars to file……");
		File envVars = new File(uploadBean.getEnvVarsPath());
		if (!envVars.getParentFile().exists() && !envVars.getParentFile().mkdirs()) {
			printMessage(listener, true, "orz..., write env vars to file failure……" + "\n");
			return;
		}
		File file = write(envVars.getAbsolutePath(), getEnvVarsInfo(pgyerBean), "utf-8");
		if (file != null)
			printMessage(listener, true, "write env vars to file success! " + file + "\n");
		else
			printMessage(listener, true, "orz..., write env vars to file failure……" + "\n");
	}

	/**
	 * 写文件
	 *
	 * @param path     文件路径，重写入
	 * @param content  文件内容
	 * @param encoding 文件编码
	 */
	private static File write(String path, String content, String encoding) {
		try {
			File file = new File(path);
			if (!file.delete() && !file.createNewFile()) return null;
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file), encoding));
			writer.write(content);
			writer.close();
			return file;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 下载文件
	 *
	 * @param urlString 地址
	 * @param savePath  存储路径
	 * @param fileName  文件名称
	 */
	private static File download(String urlString, String savePath, String fileName) {
		InputStream is = null;
		OutputStream os = null;
		try {
			File dir = new File(savePath);// 输出的文件流
			if (!dir.exists() && !dir.mkdirs()) return null;
			String filePath = savePath + File.separator + fileName;

			URL url = new URL(urlString);// 构造URL
			URLConnection con = url.openConnection();// 打开连接
			con.setConnectTimeout(60 * 1000);//设置请求超时为5s
			is = con.getInputStream();// 输入流

			byte[] bs = new byte[1024 * 8];// 8K的数据缓冲
			int len;// 读取到的数据长度

			os = new FileOutputStream(filePath);
			while ((len = is.read(bs)) != -1) {// 开始读取
				os.write(bs, 0, len);
			}
			return new File(filePath);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * @param pgyerBean
	 * @param listener
	 */
	private static void printResultInfo(PgyerBean pgyerBean, Message listener) {
		PgyerBean.DataBean data = pgyerBean.getData();
		printMessage(listener, true, "App Key：" + data.getAppKey());
		printMessage(listener, true, "应用类型：" + data.getAppType());
		printMessage(listener, true, "是否是最新版：" + data.getAppIsLastest());
		printMessage(listener, true, "App 文件大小：" + data.getAppFileSize());
		printMessage(listener, true, "应用名称：" + data.getAppName());
		printMessage(listener, true, "版本号：" + data.getAppVersion());
		printMessage(listener, true, "Android的版本编号：" + data.getAppVersionNo());
		printMessage(listener, true, "build号：" + data.getAppBuildVersion());
		printMessage(listener, true, "应用程序包名：" + data.getAppIdentifier());
		printMessage(listener, true, "应用的Icon图标key：" + data.getAppIcon());
		printMessage(listener, true, "应用介绍：" + data.getAppDescription());
		printMessage(listener, true, "应用更新说明：" + data.getAppUpdateDescription());
		printMessage(listener, true, "应用截图的key：" + data.getAppScreenshots());
		printMessage(listener, true, "应用短链接：" + data.getAppShortcutUrl());
		printMessage(listener, true, "应用二维码地址：" + data.getAppQRCodeURL());
		printMessage(listener, true, "应用上传时间：" + data.getAppCreated());
		printMessage(listener, true, "应用更新时间：" + data.getAppUpdated());
		printMessage(listener, true, "应用主页：" + data.getAppPgyerURL());
		printMessage(listener, true, "应用构建主页：" + data.getAppBuildURL());
		printMessage(listener, false, "");
	}

	/**
	 * @param pgyerBean
	 * @return
	 */
	private static String getEnvVarsInfo(PgyerBean pgyerBean) {
		StringBuffer sb = new StringBuffer();
		sb.append("appKey").append("=").append(pgyerBean.getData().getAppKey()).append("\n");
		sb.append("appType").append("=").append(pgyerBean.getData().getAppType()).append("\n");
		sb.append("appIsLastest").append("=").append(pgyerBean.getData().getAppIsLastest()).append("\n");
		sb.append("appFileSize").append("=").append(pgyerBean.getData().getAppFileSize()).append("\n");
		sb.append("appName").append("=").append(pgyerBean.getData().getAppName()).append("\n");
		sb.append("appVersion").append("=").append(pgyerBean.getData().getAppVersion()).append("\n");
		sb.append("appVersionNo").append("=").append(pgyerBean.getData().getAppVersionNo()).append("\n");
		sb.append("appBuildVersion").append("=").append(pgyerBean.getData().getAppBuildVersion()).append("\n");
		sb.append("appIdentifier").append("=").append(pgyerBean.getData().getAppIdentifier()).append("\n");
		sb.append("appIcon").append("=").append(pgyerBean.getData().getAppIcon()).append("\n");
		sb.append("appDescription").append("=").append(pgyerBean.getData().getAppDescription()).append("\n");
		sb.append("appUpdateDescription").append("=").append(pgyerBean.getData().getAppUpdateDescription()).append("\n");
		sb.append("appScreenshots").append("=").append(pgyerBean.getData().getAppScreenshots()).append("\n");
		sb.append("appShortcutUrl").append("=").append(pgyerBean.getData().getAppShortcutUrl()).append("\n");
		sb.append("appCreated").append("=").append(pgyerBean.getData().getAppCreated()).append("\n");
		sb.append("appUpdated").append("=").append(pgyerBean.getData().getAppUpdated()).append("\n");
		sb.append("appQRCodeURL").append("=").append(pgyerBean.getData().getAppQRCodeURL()).append("\n");
		sb.append("appPgyerURL").append("=").append(pgyerBean.getData().getAppPgyerURL()).append("\n");
		sb.append("appBuildURL").append("=").append(pgyerBean.getData().getAppBuildURL()).append("\n");
		return sb.toString();
	}

	/**
	 * Header
	 *
	 * @param listener
	 */
	private static void printHeaderInfo(Message listener) {
		printMessage(listener, false, "");
		printMessage(listener, false, "**************************************************************************************************");
		printMessage(listener, false, "**************************************************************************************************");
		printMessage(listener, false, "********************************          UPLOAD TO PGYER         ********************************");
		printMessage(listener, false, "**************************************************************************************************");
		printMessage(listener, false, "**************************************************************************************************\n");
	}

	/**
	 * print message
	 *
	 * @param listener
	 * @param needTag
	 * @param message
	 */
	private static void printMessage(Message listener, boolean needTag, String message) {
		if (listener == null) return;
		listener.message(needTag, message);
	}

	/**
	 * @param str
	 * @return
	 */
	private static String replaceBlank(String str) {
		String dest = "";
		if (str != null) {
			Pattern p = Pattern.compile("\\s*|\t|\r|\n");
			Matcher m = p.matcher(str);
			dest = m.replaceAll("");
		}
		return dest;
	}

	/**
	 *
	 */
	private static class FileComparator implements Comparator<String>, Serializable {
		File dir;

		public FileComparator(File dir) {
			this.dir = dir;
		}

		@Override
		public int compare(String o1, String o2) {
			File file1 = new File(dir, o1);
			File file2 = new File(dir, o2);
			if (file1.lastModified() < file2.lastModified())
				return 1;
			if (file1.lastModified() > file2.lastModified())
				return -1;
			return 0;
		}
	}
}
