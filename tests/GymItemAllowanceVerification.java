import local.pixelmon.backport.GymItemAllowance;
import static local.pixelmon.backport.GymItemAllowance.Denial.*;

/** Standalone budget scenarios: no Forge server, Pokemon, player or game turns. */
public final class GymItemAllowanceVerification {
    static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        GymItemAllowance budget=new GymItemAllowance();
        require(budget.denial(true,0,0)==NONE,"First revival must be available");
        budget.successfulUse(true);
        require(budget.remaining()==2 && budget.remainingRevivals()==0,"Revival must count within total");
        for(int i=0;i<5;i++)require(budget.denial(true,0,0)==REVIVAL,"Repeated revival must be rejected");
        require(budget.remaining()==2,"Rejected selections must leave budget untouched");
        require(budget.denial(false,0,0)==NONE,"Revival limit must not block regular healing");
        budget.successfulUse(false);budget.successfulUse(false);
        require(budget.denial(false,0,0)==TOTAL && budget.denial(true,0,0)==TOTAL,"Fourth bag use must be rejected");
        budget=new GymItemAllowance();budget.successfulUse(false);budget.successfulUse(false);
        require(budget.denial(false,1,0)==TOTAL,"Doubles cannot queue the fourth item");
        require(budget.remaining()==1,"Pending choices do not count as successful uses");
        budget=new GymItemAllowance();require(budget.denial(true,1,1)==REVIVAL,"Doubles cannot queue two revivals");
        require(budget.denial(false,1,1)==NONE,"Doubles may queue a heal beside the revival");
        require(new GymItemAllowance().remaining()==3,"Fresh battle budget is independent");
        System.out.println("PASS gym budget: 3 total including 1 revival, rejected/failed choices untouched, double-battle reservations, fresh-battle reset; no game scenarios run");
    }
}
