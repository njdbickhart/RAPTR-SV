/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructs;

/**
 *
 * @author bickhart
 */
public enum readEnum {
    // Split read
    SplitFirstHalf, 
    SplitSecondHalf, 
    // Read orientation
    FirstForward, 
    FirstReverse, 
    SecondForward, 
    SecondReverse, 
    // Read state
    IsSplit, 
    IsDisc, 
    IsUnbalanced,  
    HasOEAForward,
    HasOEAReverse,
    // Set cover state
    Used
}
