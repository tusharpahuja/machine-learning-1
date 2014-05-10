package hmm.algorithms;

import common.LogP;

import hmm.HMM;
import hmm.State;


import pair.Pair;

/**
 * Implementation of the Forward algorithm for calculating the full probability
 * of a sequence generated by a hidden Markov model.
 * 
 * @author matthewbernstein
 *
 */
public class ForwardAlgorithm 
{	
	public static int debug = 0;
	
	/**
	 * Given a HMM model and sequence generated from the HMM, the forward 
	 * algorithm computes the full probability of the sequence under the 
	 * model.  The algorithm also returns the full dynamic programming matrix.
	 * The (i,j) entry in the matrix is the joint probability of generating
	 * j characters in the sequence and being in state i.
	 * 
	 * @param model the hidden Markov model object
	 * @param sequence the sequence produced by the HMM
	 * @return the full probability of the sequence as well as the dynamic
	 * programming matrix
	 */
	public static Pair<Double, DpMatrix> run(HMM model, String sequence)
	{
		DpMatrix dpMatrix = new DpMatrix(model, sequence);
	
		/*
		 *  Initialize the matrix
		 */
		intitialize(dpMatrix, model);
		
		/*
		 *  Run the algorithm
		 */
		Double finalProb = runIteration(dpMatrix, model, sequence);
		
		return new Pair<Double, DpMatrix>(finalProb, dpMatrix);
	}

	private static Double runIteration(DpMatrix dpMatrix, 
									  HMM model, 
									  String sequence)
	{	
		for (int t = 1; t < dpMatrix.getNumColumns(); t++)
		{				
			/*
			 * Iterate through all non-silent states
			 */
			for (State currState : model.getStates())
			{
				if (!currState.isSilent())
				{
					/*
					 *  The emission probability of the current symbol at the 
					 *  ith time step.
					 */
					double eProb = model.getEmissionProb(currState.getId(), 
							  			Character.toString(sequence.charAt(t-1)));
					
					/*
					 * Sum over previous time-step
					 */
					double sum = Double.NaN;
					for (State lastState : model.getStates())
					{
						double fValue = dpMatrix.getValue(lastState, t-1);
						
						double tProb  = model.getTransitionProb(lastState.getId(), 
															    currState.getId());	
						
						sum = LogP.sum(sum, LogP.prod(fValue, tProb));
					}	
					
					double newFValue = LogP.prod(sum, eProb);
											
					/*
					 *  Set the new value in the DP matrix
					 */
					dpMatrix.setValue(currState, t, newFValue);
				}
			}
			
			/*
			 * Iterate through all silent states
			 */
			for (State currState : model.getSortedSilentStates())
			{
				/*
				 * Sum over previous time-step
				 */
				double sum = Double.NaN;
				for (State lastState : model.getStates())
				{
					double fValue = dpMatrix.getValue(lastState, t);
					
					double tProb  = model.getTransitionProb(lastState.getId(), 
														    currState.getId());
					sum = LogP.sum(sum, LogP.prod(fValue, tProb));
				}
				
				//if (t == 1) System.out.println("State " + currState.getId() + " TIME " + " SUM: " + sum);
				
				double newFValue = sum;
					
				/*
				 *  Set the new value in the DP matrix
				 */
				dpMatrix.setValue(currState, t, newFValue);
			}
			
			if (debug > 1)
			{
				System.out.println("TIME STEP : " + t);
				System.out.println(dpMatrix);
			}
		}
		
		/*
		 * Compute the final probability of the sequence by summing over the
		 * joint probability of observing the sequence (i.e. of being in the 
		 * last time step) in each state.
		 */
		double sum = Double.NaN;
		for (State state : model.getStates())
		{	
			if (!state.isSilent())
			{
				double fValue = dpMatrix.getValue(state, dpMatrix.getNumColumns() - 1);
				sum = LogP.sum(sum, fValue);
			}
		}
		
		return sum;
	}
	
	/**
	 * Initialize the dynamic programming matrix
	 * 
	 * @param dpMatrix the dynamic programming matrix object
	 * @param model the HMM object
	 */
	private static void intitialize(DpMatrix dpMatrix, HMM model)
	{
		
		/*
		 *  Set all elements to 0.0
		 */
		for (State state : model.getStateContainer().getStates())
		{
			dpMatrix.setValue(state, 0, Double.NaN);
		}
		
		/*
		 *  Set coordinate (0,0) to 1.0 corresponding to 100% probability
		 *  that we are in the begin state at time step 0
		 */
 		State beginState = model.getBeginState();
		dpMatrix.setValue(beginState, 0, LogP.ln(1.0));
		
		/*
		 *  Set initial probabilities for silent states
		 */
		for (State currState : model.getSortedSilentStates())
		{
			if (currState != beginState)
			{
				/*
				 * Sum over first time-step
				 */
				double sum = Double.NaN;
				for (State lastState : model.getStates())
				{
					double fValue = dpMatrix.getValue(lastState, 0);

					double tProb  = model.getTransitionProb(lastState.getId(), 
														    currState.getId());
										
					sum = LogP.sum(sum, LogP.prod(fValue, tProb));
				}	
				double newFValue = sum;
					
				/*
				 *  Set the new value in the DP matrix
				 */
				dpMatrix.setValue(currState, 0, newFValue);
			}	
		}
		
		if (debug > 1)
		{
			System.out.println("TIME STEP : 0 (Init)");
			System.out.println(dpMatrix);
		}
	}
	
}