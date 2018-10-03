package interfaces;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public interface LinksFixer {

	public boolean fixQuestionsFromTerm(long prev_enrollment_term);
	ResultSet getAttachmentMigratedFromAltImage(String alt, long new_course_id);

	public class Fixer implements LinksFixer {

		private Connection conn = null;

		public Fixer(Connection conn) {
			this.conn = conn;
		}

		/*
		 * return true if fixed, else false
		 * */
		public boolean fixQuestionsFromTerm(long prev_enrollment_term) {

        try {
        	ResultSet queryResult;
            Statement stmtQuery = conn.createStatement();
            Statement stmtUpdater = conn.createStatement();

            queryResult = stmtQuery.executeQuery("SELECT q.context_id as course_id,qq.* from quiz_questions qq join quizzes q on qq.quiz_id=q.id "
                                     + "WHERE quiz_id in (SELECT id from quizzes WHERE context_id in (SELECT id from courses WHERE enrollment_term_id="+prev_enrollment_term+")) "
                                     + "AND question_data like '%/courses/%' and assessment_question_id is not null ORDER BY id");
            while(queryResult.next()) {
                String question_data = queryResult.getString("question_data");
                ArrayList<String> data_lines = new ArrayList<String>(Arrays.asList(question_data.split("\n")));
                for (int i = 0; i < data_lines.size(); i++) {
                    if(data_lines.get(i).contains("/courses/")) {
                        String old_html = data_lines.get(i);
                        question_data = question_data.replace(old_html, modificarHTML(old_html,queryResult.getLong("course_id")));
                        System.out.println(old_html);
                    }
						
                }
                PreparedStatement updateStmt = conn.prepareStatement("UPDATE quiz_questions SET question_data=? WHERE id=?");
                updateStmt.setString(1, question_data);
                updateStmt.setLong(2, queryResult.getLong("id"));
                updateStmt.executeUpdate();
            }
			
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
        }
			return true;
		}

    private String modificarHTML(String html_original,long new_course_id) throws SQLException {
    	boolean isNewCourse = true;
        Statement stmt = conn.createStatement();
        ResultSet rs = null;
        if(html_original==null) return null;
        if(html_original.contains("href")) {
            String html_new = new String(html_original);
            Document doc = Jsoup.parse(html_original);
            Elements anchors = doc.getElementsByTag("a");
            for (Element link : anchors){
                String href = link.attr("href");
                String alt = link.attr("alt");
                String title = link.attr("title");
                String href_new = "";
                String link_id_orig=null;
                int posCourse,posLink;
                ArrayList<String> href_parts = new ArrayList(Arrays.asList(href.split("/")));
                posCourse=href_parts.indexOf("courses");
                if(posCourse>=0 && posCourse<href_parts.size()-1) {
                    try {
                        Long.parseLong(href_parts.get(posCourse+1));//valida que tenga un id, sino entra al catch y no cambia nada
                        System.out.println(href_parts.get(posCourse + 1));
                        if(new Long(href_parts.get(posCourse+1)).longValue() == new_course_id) {
                        	isNewCourse = true;
                        } else {
                        	isNewCourse = false;
                        	href_parts.set(posCourse+1, ""+new_course_id);
                        }
                    }catch(NumberFormatException e){}
                    //href_parts.set(posCourse+1, ""+course_id_new);
                    posLink=href_parts.indexOf("files");
                    if(posLink>=0) {
                        if(posLink<href_parts.size()-1) {
                            try {
                            	if(!isNewCourse) {
                            		Long.parseLong(href_parts.get(posLink+1));//valida que tenga un id, sino entra al catch y no cambia nada
                            		link_id_orig=href_parts.get(posLink+1);
                            		rs = stmt.executeQuery("SELECT id FROM attachments WHERE migration_id='"+link_id_orig+"'");
                            		if(rs.next())	href_parts.set(posLink+1, rs.getString("id"));
                            		else			System.err.println("No se modifico el attachment_id en el link del curso:"+new_course_id);
                                //href_parts.set(posLink+1, ""+oAttachment.getAttachmentsIds().get(Long.parseLong(link_id_orig)));
                            	} else {
                            		System.out.println(title);
                            		System.out.println(alt);
                            		ResultSet getAttachmentResult ;
                            		if(alt.isEmpty()) {
                            			getAttachmentResult= this.getAttachmentMigratedFromAltImage(title, new_course_id);
                            		} else {
                            			getAttachmentResult = this.getAttachmentMigratedFromAltImage(alt, new_course_id);
                            		}
                            		if(getAttachmentResult.next()) {
                            			System.out.println("Actualizando attachment ya migrado..."+ getAttachmentResult.getString("display_name"));
                            			href_parts.set(posLink + 1, getAttachmentResult.getString("id"));
                            	} else System.err.println("No existe el attachment en el curso:"+new_course_id+ "  " + alt);
                            	}
                            }catch(NumberFormatException e){}
                        }
                    }
                    else {
                        posLink=href_parts.indexOf("wiki");
                        if(posLink>=0) {
                            if(posLink<href_parts.size()-1) {
                                try {
                                    Long.parseLong(href_parts.get(posLink+1));//valida que tenga un id, sino entra al catch y no cambia nada
                                    link_id_orig=href_parts.get(posLink+1);
                                    rs = stmt.executeQuery("SELECT id FROM wiki_pages WHERE migration_id='"+link_id_orig+"'");
                                    if(rs.next())	href_parts.set(posLink+1, rs.getString("id"));
                                    else			System.err.println("No se modifico el wiki_page_id en el link del curso:"+new_course_id);
                                    //									href_parts.set(posLink+1, ""+oWiki.getIds().get(Long.parseLong(link_id_orig)));
                                }catch(NumberFormatException e){}
                            }
                        }
                        else {
                            posLink=href_parts.indexOf("assignments");
                            if(posLink>=0) {
                                if(posLink<href_parts.size()-1) {
                                    try {
                                        Long.parseLong(href_parts.get(posLink+1));//valida que tenga un id, sino entra al catch y no cambia nada
                                        link_id_orig=href_parts.get(posLink+1);
                                        rs = stmt.executeQuery("SELECT id FROM assignments WHERE migration_id='"+link_id_orig+"'");
                                        if(rs.next())	href_parts.set(posLink+1, rs.getString("id"));
                                        else			System.err.println("No se modifico el assignment_id en el link del curso:"+new_course_id);
                                    }catch(NumberFormatException e){}
                                }
                            }
                            else {
                                posLink=href_parts.indexOf("quizzes");
                                if(posLink>=0) {
                                    if(posLink<href_parts.size()-1) {
                                        try {
                                            Long.parseLong(href_parts.get(posLink+1));//valida que tenga un id, sino entra al catch y no cambia nada
                                            link_id_orig=href_parts.get(posLink+1);
                                            rs = stmt.executeQuery("SELECT id FROM quizzes WHERE migration_id='"+link_id_orig+"'");
                                            if(rs.next())	href_parts.set(posLink+1, rs.getString("id"));
                                            else			System.err.println("No se modifico el quiz_id en el link del curso:"+new_course_id);
                                        }catch(NumberFormatException e){}
                                    }
                                }
                                //								else href_new=null;//si es link ha otra cosa
                            }
                        }
                    }
                }
                if(href_new!=null) {
                    for(int i=0;i<href_parts.size();i++) {
                        if(i==href_parts.size()-1) {
                            href_new+=href_parts.get(i);
                        }
                        else {
                            href_new+=href_parts.get(i)+"/";
                        }
                    }
                    html_new=html_new.replace(href, href_new);
                }
            }
            return html_new.replaceAll("'", "''");
        }
        else if(html_original.contains("<img ")) {
            String html_new = new String(html_original);
            Document doc = Jsoup.parse(html_original);
            Elements anchors = doc.getElementsByTag("img");
            for (Element link : anchors){
                String src = link.attr("src");
                String src_new = "";
                String link_id_orig=null;
                int posCourse,posLink;
                ArrayList<String> src_parts = new ArrayList<String>(Arrays.asList(src.split("/")));
                posCourse=src_parts.indexOf("courses");
                String alt = link.attr("alt");

                if(posCourse>=0 && posCourse<src_parts.size()-1) {
                    try {
                        Long.parseLong(src_parts.get(posCourse+1));//valida que tenga un id, sino entra al catch y no cambia nada

                        if(new Long(src_parts.get(posCourse+1)).longValue() == new_course_id) {
                        	isNewCourse = true;
                        } else isNewCourse = false;
                        src_parts.set(posCourse+1, ""+new_course_id);
                    }catch(NumberFormatException e){}
                    //href_parts.set(posCourse+1, ""+course_id_new);
                    posLink=src_parts.indexOf("files");
                    if(posLink>=0 && posLink<src_parts.size()-1) {
                        try {
                            Long.parseLong(src_parts.get(posLink+1));//valida que tenga un id, sino entra al catch y no cambia nada
                            link_id_orig=src_parts.get(posLink+1);
                            if(!isNewCourse)  {
                            rs = stmt.executeQuery("SELECT new_id FROM tem_attachment_ids WHERE old_id="+link_id_orig);
                            if(rs.next()) {
                                long root_id = rs.getLong("new_id");
                                rs = stmt.executeQuery("SELECT * FROM attachments WHERE id = "+root_id);
                                if(rs.next()) {
                                    if(rs.getLong("root_attachment_id")==0) root_id = rs.getLong("id");
                                    else  root_id = rs.getLong("root_attachment_id");
									
                                    rs = stmt.executeQuery("SELECT * FROM attachments WHERE root_attachment_id="+root_id+" and context_type='Course' AND context_id="+new_course_id);
                                    if(rs.next()) {
                                        src_parts.set(posLink+1, rs.getString("id"));
                                    }
                                    else System.err.println("No existe el attachment:"+root_id);
                                }
                            }
                            else	System.err.println("No se modifico el attachment_id en el link del curso:"+new_course_id);
                            } else {
                            	System.out.println(alt);
                            	ResultSet getAttachmentResult = this.getAttachmentMigratedFromAltImage(alt, new_course_id);
                            	if(getAttachmentResult.next()) {
                            		System.out.println("Actualizando attachment ya migrado..."+ getAttachmentResult.getString("display_name"));
                            		src_parts.set(posLink + 1, getAttachmentResult.getString("id"));
                            	} else System.err.println("No existe el attachment en el curso:"+new_course_id+ "  " + alt);
                            }
                        }catch(NumberFormatException e){}
                    }
                }
                if(src_new!=null) {
                    for(int i=0;i<src_parts.size();i++) {
                        if(i==src_parts.size()-1) {
                            src_new+=src_parts.get(i);
                        }
                        else {
                            src_new+=src_parts.get(i)+"/";
                        }
                    }
                    html_new=html_new.replace(src, src_new);
                }
            }
            return html_new.replaceAll("'", "''");
        }
        else
            return html_original.replaceAll("'", "''");
    }

	public ResultSet getAttachmentMigratedFromAltImage(String alt, long new_course_id) {
		ResultSet getAttachmentMigrated;
		PreparedStatement stmtGetAttachment;
		
		String sql = "select * from attachments where context_type='Course' and context_id=? and display_name=?";
		
		try {
			stmtGetAttachment = conn.prepareStatement(sql);
			stmtGetAttachment.setLong(1, new_course_id);
			stmtGetAttachment.setString(2, alt);
			getAttachmentMigrated = stmtGetAttachment.executeQuery();
			return getAttachmentMigrated;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
	}

	public void fixEnrollmentNamesOfTerm(int term) {
		String sqlMigUsuarios = "select * from mig_usuarios where ";
	}
	
	public void fixFilesInWikiPages(int term) {
		String sqlCourses = "Select id from courses where enrollment_term_id="+term
				+" and id=20190"
				;

		ResultSet getCourses;
		Statement stmtGetCourses;
		try {
			stmtGetCourses = conn.createStatement();

        getCourses = stmtGetCourses.executeQuery(sqlCourses);
            while(getCourses.next()) {
            	try {
            		long course_id = getCourses.getLong("id");
            		String sqlWikiPages = "select * from wiki_pages where wiki_id="
            				+ "(select wiki_id from courses where id="+course_id+") and body is not null";
            		ResultSet rsWikiPages;
            		Statement stmtGetWikiPages;
            		
            		stmtGetWikiPages = conn.createStatement();

            		rsWikiPages = stmtGetWikiPages.executeQuery(sqlWikiPages);
            		
            		while(rsWikiPages.next()) {
            			try {
            				String body = rsWikiPages.getString("body");
            				ArrayList<String> data_lines = new ArrayList<String>(Arrays.asList(body.split("\n")));
            				for (int i = 0; i < data_lines.size(); i++) {
            					if(data_lines.get(i).contains("/courses/")) {
            						String old_html = data_lines.get(i);
            						body = body.replace(old_html, modificarHTML(old_html,course_id));
            						System.out.println(old_html);
            					}
						
            				}
            				PreparedStatement updateStmt = conn.prepareStatement("UPDATE wiki_pages SET body=? WHERE id=?");
            				updateStmt.setString(1, body);
            				updateStmt.setLong(2, rsWikiPages.getLong("id"));
            				updateStmt.executeUpdate();
            			} catch(Exception e) {
            				e.printStackTrace();
            			}
            		}
            	} catch(Exception e) {
            		e.printStackTrace();
            	}
            }
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void fixContentTagFilesFromTerm(int term) {
		
	}
	
	public void repairAttachmentsFromTerm(long term_id) {
		
	}

	public void repairAttachmentsFromCourse(long course_id) {
		
	}
	}
}
