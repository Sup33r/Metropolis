package live.supeer.metropolis;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class Leaderboard {

    private UUID creatorUUID;
    private Long createDate;
    private String type;
    private String[] conditions;

    public Leaderboard(UUID creatorUUID, Long createDate, String type, String[] conditions) {
        this.creatorUUID = creatorUUID;
        this.createDate = createDate;
        this.type = type;
        this.conditions = conditions;
    }

    public void addCondition(String condition) {
        if (conditions == null) {
            conditions = new String[]{condition};
        } else {
            String[] newConditions = new String[conditions.length + 1];
            System.arraycopy(conditions, 0, newConditions, 0, conditions.length);
            newConditions[conditions.length] = condition;
            conditions = newConditions;
        }
    }

    public void removeCondition(String condition) {
        if (conditions == null || conditions.length == 0) {
            return;
        }
        String[] newConditions = new String[conditions.length - 1];
        int j = 0;
        for (String s : conditions) {
            if (s.equals(condition)) {
                continue;
            }
            newConditions[j++] = s;
        }
        conditions = newConditions;
    }
}
