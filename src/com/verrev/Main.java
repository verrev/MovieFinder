package com.verrev;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

public class Main {
	private static String pat = "href=\"(http://us.imdb.com/title/.+?)\">";
	private static String ratPat = "<div class=\"titlePageSprite star-box-giga-star\"> (.+?) </div>";
	private static String namePat = "<title>(.+?) \\(([0-9]+)\\) - IMDb</title>";
	private static String genrePat = "<span class=\"itemprop\" itemprop=\"genre\">(.+?)</span>";
	
	public static void main(String[] args) throws Exception {
		int i = 0;
		while (i < 500) { // parse the first 500 pages
			SessionFactory sf = new AnnotationConfiguration().configure().buildSessionFactory();
			Session ses = sf.openSession();
			ses.beginTransaction();
			
			
			++i;
			String s = getWebContents("http://www.subclub.eu/jutud.php?&page=" + i);
			Matcher m = Pattern.compile(pat).matcher(s);
			while (m.find()) {			
				lift(ses, m.group(1));
			}	
			
			ses.getTransaction().commit();
			ses.close();
			sf.close();
			System.out.println(i);
		}
	}
	public static void lift(Session ses, String url) throws Exception {
		try {
			String s = getWebContents(url);
			Movie mov = new Movie();
			
			Matcher m = Pattern.compile(ratPat).matcher(s);
			if (m.find()) {				
				mov.rating = Double.parseDouble(m.group(1));
			}
			m = Pattern.compile(namePat).matcher(s);
			if (m.find()) {				
				mov.title = m.group(1);
				mov.year = Integer.parseInt(m.group(2));
			}
			m = Pattern.compile(genrePat).matcher(s);
			while (m.find()) {				
				mov.genre += m.group(1) + ", ";
			}
			if (mov.genre.length() > 0) {
				mov.genre = mov.genre.substring(0, mov.genre.length() - 2);
				
				if (!alreadyExists(ses, mov)) {
					ses.save(mov);
				}
			}
		} catch (Exception ex) {
			return;
		}
	}
	public static String getWebContents(String site) throws Exception {
		URL u = new URL(site);
		BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
		String inputLine, s = "";
		while ((inputLine = in.readLine()) != null)
			s += inputLine + "\n";
		in.close();
		return s;
	}
	public static boolean alreadyExists(Session ses, Movie m) {
		return !ses.createQuery("from Movie where title=:t and year=:y").setParameter("t", m.title).setParameter("y", m.year).list().isEmpty();
	}
}
