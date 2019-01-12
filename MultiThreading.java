package unassignedBranchDeterminer;

import java.util.List;

public class MultiThreading extends MySwingWorker<Void, String>
{
	private GUIDisplay guiObject;

	public MultiThreading(GUIDisplay guiObject) {
		this.guiObject = guiObject;
	}

	@Override
	protected Void doInBackground() throws Exception
	{
		new ExcelIO(guiObject);

		return null;
	}

	@Override
	protected void done()
	{
		guiObject.disposeProgressBar();
	}

	@Override
	protected void process(List<String> chunks)
	{
		guiObject.process(chunks);
	}
}
