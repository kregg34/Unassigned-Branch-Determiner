package unassignedBranchDeterminer;

@SuppressWarnings("serial")
public class CantFindLineException extends Exception
{
	public CantFindLineException() 
	{
		super("Could not find the line.");
	}
}
