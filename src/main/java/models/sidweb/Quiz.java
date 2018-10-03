package models.sidweb;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

public class Quiz {
	@Nullable
	private int id;
	
	@NotNull
	private String title;
	
	@NotNull
	private String description;
	
	@Nullable
	private String quiz_data;
	
	@NotNull
	private double points_possible;
	
	@NotNull
	private int context_id;
	
	@NotNull
	private String context_type;
	
	@NotNull
	private int assignment_id;
	
	@NotNull
	private String workflow_state;
	
	@NotNull
	private int shuffle_answers;
	
	@NotNull
	private boolean show_correct_answers; 
	
	@Nullable
	private int time_limit; 
	
	@NotNull
	private int allowed_attempts; 
	
	@NotNull
	private String scoring_policy; 
	
}
