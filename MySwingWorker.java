package unassignedBranchDeterminer;

import javax.swing.SwingWorker;

public abstract class MySwingWorker<T, V> extends SwingWorker<T, V> 
{
    @SafeVarargs
	public final void myPublish(V... args) 
    {
        publish(args);
    }
}
