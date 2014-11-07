
package chatty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import chatty.util.api.Follower;
import chatty.util.api.FollowerInfo;
import chatty.util.api.FollowerManager;
import chatty.util.api.TwitchApi;


public class FollowersThread extends Thread
{
	public static int subs_start = -100;
	public static int subs_last = -100;
	public static int subs_diff = -100;

	public static int foll_start = -100;
	public static int foll_last = -100;
	public static int foll_diff = -100;

	public static int offset = 0;
	public static int time = 0;

	public static String subcounttext = "Sub today:";
	public static String sublasttext = "Last sub:";

	public static int subs_old = -100;

	public static int foll_old = -100;

	public static boolean firstfol = true;

	public static boolean firstsub = true;

	TwitchApi api;

	String stream;

	public enum Type {
		FOLLOWERS("Followers"), SUBSCRIBERS("Subscribers");

		private final String name;

		Type(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private final Type type;

	FontRenderer fr;

	public FollowersThread(Type type, TwitchApi api, String stream) 
	{
		System.out.println("Init FollowersThread for " + stream);

		this.stream = stream;
		this.api = api;
		this.type = type;

		String fs = readFileAsString(System.getProperty("user.dir")+"\\config.txt");

		fr = new FontRenderer();

		try
		{
			offset = Integer.parseInt(fs.split("offset:")[1].split(";")[0]);
			time = Integer.parseInt(fs.split("time:")[1].split(";")[0]);

			subcounttext = fs.split("todaytext:\"")[1].split("\";")[0];

			sublasttext = fs.split("lasttext:\"")[1].split("\";")[0];

			System.out.println(subcounttext);
			System.out.println(sublasttext);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public void run() 
	{

		while(true)
		{

			if (type == Type.FOLLOWERS)
			{
				api.getFollowers(stream);
				//api.requestFollowers(stream);
			}

			if (type == Type.SUBSCRIBERS)
			{
				api.getSubscribers(stream);
				//api.requestFollowers(stream);
			}

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			//System.out.println("FollowersThread loop " + FollowerManager.lastFolInfo);

			//System.out.println("FollowersThread loop ");

			updateStats();
		}

	}


	/**
	 * Update the stats label.
	 */
	private void updateStats() 
	{

		if ( (type == Type.FOLLOWERS) && (FollowerManager.lastFolInfo != null) )
		{
			String st = FollowerManager.lastFolInfo.toString();

			//System.out.println(st);

			foll_old = foll_last;

			String count = st.split(" ")[0];

			foll_last = Integer.parseInt(count);

			if ( (foll_last != foll_old) || firstfol)
			{
				firstfol = false;

				String last = st.split(",")[0].split(" ")[1].substring(1);

				String[] lasts = st.substring(st.indexOf("[")+1).split(", ");
				String[] lasts100 = st.substring(st.indexOf("[")+1).split(", ");

				int lastc = 3;

				StringBuilder sb = new StringBuilder("");
				StringBuilder sb100 = new StringBuilder("");

				sb.append(lasts[0]);

				for (int i = 1; i < lastc; i++)
				{
					sb.append("\n");
					sb.append(lasts[i]);
					//sb.append(" ");
				}

				sb100.append(lasts100[0]);

				for (int i = 1; i < lasts100.length; i++)
				{
					sb100.append("\n");
					sb100.append(lasts100[i]);
					//sb.append(" ");
				}



				if (foll_start == -100) 
				{
					foll_start = Integer.parseInt(count);
				}



				foll_diff = foll_last - foll_start;

				writeFileAsString(System.getProperty("user.dir")+"\\" + stream + "_fol_total.txt", count);

				writeFileAsString(System.getProperty("user.dir")+"\\" + stream + "_fol_fromstart.txt", Integer.toString(foll_diff));

				writeFileAsString(System.getProperty("user.dir")+"\\" + stream + "_fol_last.txt", last);

				writeFileAsString(System.getProperty("user.dir")+"\\" + stream + "_fol_lasts.txt", sb.toString());
				writeFileAsString(System.getProperty("user.dir")+"\\" + stream + "_fol_lasts100.txt", sb100.toString());

				String bytime = Stats.makeDayStats(FollowerManager.lastFolInfo);

				writeFileAsString(System.getProperty("user.dir")+"\\" + stream + "_fol_day.txt", bytime);

				fr.display("Last Fol:"+last, System.getProperty("user.dir")+"\\" + stream + "_fol_last.png");

				fr.display("Fol today:"+bytime, System.getProperty("user.dir")+"\\" + stream + "_fol_day.png");
			}
		}

		if ( (type == Type.SUBSCRIBERS) && (FollowerManager.lastSubInfo != null))
			//if (type == Type.SUBSCRIBERS)
		{
			String st = FollowerManager.lastSubInfo.toString();

			//System.out.println(st);

			subs_old = subs_last;

			String count = st.split(" ")[0];

			subs_last = Integer.parseInt(count);

			if ((subs_last != subs_old) || firstfol)
			{

				firstsub = false;

				String last = st.split(",")[0].split(" ")[1].substring(1);

				String[] lasts = st.substring(st.indexOf("[")+1).split(", ");
				String[] lasts100 = st.substring(st.indexOf("[")+1).split(", ");

				int lastc = 3;

				StringBuilder sb = new StringBuilder("");
				StringBuilder sb100 = new StringBuilder("");

				sb.append(lasts[0]);

				for (int i = 1; i < lastc; i++)
				{
					sb.append("\n");
					sb.append(lasts[i]);
					//sb.append(" ");
				}


				sb100.append(lasts100[0]);

				for (int i = 1; i < lasts100.length; i++)
				{
					sb100.append("\n");
					sb100.append(lasts[i]);
				}

				if (foll_start == -100) 
				{
					subs_start = Integer.parseInt(count);
				}




				subs_diff = subs_last - subs_start + offset;

				writeFileAsString(System.getProperty("user.dir")+"\\" + stream + "_sub_total.txt", count);

				writeFileAsString(System.getProperty("user.dir")+"\\" + stream + "_sub_fromstart.txt", Integer.toString(subs_diff));

				writeFileAsString(System.getProperty("user.dir")+"\\" + stream + "_sub_last.txt", last);

				writeFileAsString(System.getProperty("user.dir")+"\\" + stream + "_sub_lasts.txt", sb.toString());

				writeFileAsString(System.getProperty("user.dir")+"\\" + stream + "_sub_lasts100.txt", sb100.toString());

				String bytime = Stats.makeDayStats(FollowerManager.lastSubInfo);

				writeFileAsString(System.getProperty("user.dir")+"\\" + stream + "_sub_day.txt", bytime);

				fr.display(sublasttext+last, System.getProperty("user.dir")+"\\" + stream + "_sub_last.png");

				fr.display(subcounttext+bytime, System.getProperty("user.dir")+"\\" + stream + "_sub_day.png");

				//System.out.println("DS : " + Stats.makeDayStats(FollowerManager.lastFolInfo));
			}
		}
	}

	boolean writeFileAsString(String file,String s)
	{

		try
		{
			File f = new File(file);
			f.getParentFile().mkdirs(); 
			f.createNewFile();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}


		try 
		{
			//Writer wout = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
			OutputStreamWriter wout = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");

			try
			{
				wout.write(s);
				//wout.close();
				wout.flush();
			}
			finally
			{
				wout.close();
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}

	static String readFileAsString(String s) 
	{
		try 
		{
			FileInputStream fileinputstream = new FileInputStream(s);
			InputStreamReader inputstreamreader = new InputStreamReader(fileinputstream, "UTF-8");
			BufferedReader br = new BufferedReader(inputstreamreader);

			try
			{
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();

				while (line != null) {
					sb.append(line);
					sb.append(System.lineSeparator());
					line = br.readLine();
				}

				br.close();

				return sb.toString();

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return "";
	}

	/**
	 * Helper class making stats out of a FollowerInfo.
	 */
	public static class Stats {

		private static String makeDayStats(FollowerInfo info) 
		{
			if (info.requestError) 
			{
				return "";
			}

			StringBuilder b = new StringBuilder();
			List<Follower> followers = info.followers;

			b.append(statsForTimeShort(followers, 60*time));

			return b.toString();
		}

		private static String statsForTimeShort(List<Follower> followers, int seconds) {
			boolean ok = false;
			for (int i = followers.size() - 1; i >= 0; i--) {
				Follower f = followers.get(i);
				if ((System.currentTimeMillis() - f.time) / 1000 > seconds) {
					ok = true;
				} else if (ok) {
					return  "" + (i + 1);
				} else {
					return "" +i+"+";
				}
			}
			return "0";
		}

	}
}
