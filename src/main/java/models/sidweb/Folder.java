package models.sidweb;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

public class Folder {

	@Nullable
	public long id;
	
	@NotNull
	public String name;
	
	@NotNull
	public String full_name;
	
	@NotNull
	public long context_id;
	
	@NotNull
	public String context_type;
	
	@Nullable
	public long parent_folder_id;
	
	@NotNull
	public String workflow_state;

	@Nullable
	public int position;

	@Nullable
	public long migration_id;

	public Folder(long id, String name, String full_name, long context_id, String context_type, long parent_folder_id,
			String workflow_state, int position, long migration_id) {
		super();
		this.id = id;
		this.name = name;
		this.full_name = full_name;
		this.context_id = context_id;
		this.context_type = context_type;
		this.parent_folder_id = parent_folder_id;
		this.workflow_state = workflow_state;
		this.position = position;
	}
}
