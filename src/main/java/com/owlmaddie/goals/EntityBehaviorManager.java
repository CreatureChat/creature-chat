// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.goals;

import com.owlmaddie.network.ServerPackets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Predicate;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

/**
 * The {@code EntityBehaviorManager} class now directly interacts with the goal selectors of entities
 * to manage goals, while avoiding concurrent modification issues.
 */
public class EntityBehaviorManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");

    public static void addGoal(Mob entity, Goal goal, GoalPriority priority) {
        if (!(entity.level() instanceof ServerLevel)) {
            LOGGER.debug("Attempted to add a goal in a non-server world. Aborting.");
            return;
        }

        ServerPackets.serverInstance.execute(() -> {
            GoalSelector goalSelector = GoalUtils.getGoalSelector(entity);

            // First clear any existing goals of the same type to avoid duplicates
            clearAndRemove(g -> goal.getClass().equals(g.getClass()), goalSelector);

            // Handle potential priority conflicts before adding the new goal
            moveConflictingGoals(goalSelector, priority);

            // Now add the new goal at the specified priority
            goalSelector.addGoal(priority.getPriority(), goal);
            LOGGER.debug("Goal of type {} added with priority {}", goal.getClass().getSimpleName(), priority);
        });
    }

    public static void clearAndRemove(Predicate<Goal> predicate, GoalSelector goalSelector) {
        // Collect all goals that need to be removed
        List<WrappedGoal> toBeRemoved = goalSelector.getAvailableGoals().stream()
                .filter(prioritizedGoal -> predicate.test(prioritizedGoal.getGoal()))
                .collect(Collectors.toList());

        // Stop if running and remove each goal
        toBeRemoved.forEach(prioritizedGoal -> {
            goalSelector.removeGoal(prioritizedGoal.getGoal());  // Remove the goal
        });
    }

    public static void removeGoal(Mob entity, Class<? extends Goal> goalClass) {
        ServerPackets.serverInstance.execute(() -> {
            GoalSelector goalSelector = GoalUtils.getGoalSelector(entity);
            // First clear any existing goals of the same type to avoid duplicates
            clearAndRemove(g -> goalClass.equals(g.getClass()), goalSelector);
            LOGGER.debug("All goals of type {} removed.", goalClass.getSimpleName());
        });
    }

    public static void moveConflictingGoals(GoalSelector goalSelector, GoalPriority newGoalPriority) {
        // Collect all prioritized goals currently in the selector.
        List<WrappedGoal> sortedGoals = goalSelector.getAvailableGoals().stream()
                .sorted(Comparator.comparingInt(WrappedGoal::getPriority))
                .collect(Collectors.toList());

        // Check if there is an existing goal at the new priority level.
        boolean conflictExists = sortedGoals.stream()
                .anyMatch(pg -> pg.getPriority() == newGoalPriority.getPriority());

        // If there is a conflict, we need to shift priorities of this and all higher priorities.
        if (conflictExists) {
            int shiftPriority = newGoalPriority.getPriority();
            for (WrappedGoal pg : sortedGoals) {
                if (pg.getPriority() >= shiftPriority) {
                    // Remove the goal and increment its priority.
                    goalSelector.removeGoal(pg.getGoal());
                    goalSelector.addGoal(shiftPriority + 1, pg.getGoal());
                    shiftPriority++;  // Update the shift priority for the next possible conflict.
                }
            }

            LOGGER.debug("Moved conflicting goals starting from priority {}", newGoalPriority);
        } else {
            LOGGER.debug("No conflicting goal at priority {}, no action taken.", newGoalPriority);
        }
    }
}