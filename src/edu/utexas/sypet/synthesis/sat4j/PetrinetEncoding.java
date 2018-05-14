/*
 * Copyright (C) 2017 The SyPet Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.utexas.sypet.synthesis.sat4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import edu.utexas.sypet.synthesis.PathFinder;
import edu.utexas.sypet.synthesis.model.Pair;
import edu.utexas.sypet.synthesis.model.Trio;
import edu.utexas.sypet.synthesis.sat4j.Constraint.ConstraintType;
import edu.utexas.sypet.synthesis.sat4j.Constraint.EncodingType;
import uniol.apt.adt.pn.Flow;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

/**
 * SAT-based encoding for petriNet. Modification based on Ruben's code
 * 
 * @author yufeng
 * @author Ruben
 */
public class PetrinetEncoding {

	/**
	 * Members of Encoding class
	 */
	protected List<Constraint> problem_constraints;
	protected List<Constraint> conflict_constraints;

	/**
	 * Part 1: FunctionVar; Part 2: PlaceVar
	 */
	protected List<Variable> list_variables;

	protected List<FunctionVar> list_fn_vars;

	protected List<PlaceVar> list_place_vars;

	protected Constraint objective_function;
	protected List<Integer> objective_values;

	protected Map<Pair<Transition, Integer>, Variable> functionMap_vars;

	protected Map<Trio<Place, Integer, Integer>, Variable> placeMap_vars;

	protected int objective_id;

	protected PetriNet petrinet;

	protected List<Place> inits;

	protected Place goal;

	// Max timeline: t0, t1, ... tk
	protected int maxTimeline;

	// Max token, now I am using a global value, will change it later
	protected int maxToken;

	// hack eq!
	protected List<FunctionVar> list_eq_fn_vars;
	protected Map<FunctionVar, FunctionVar> map_eq_fn_vars;
	protected List<FunctionVar> model_eq;
	protected Map<FunctionVar, Integer> map_eq_fn_activity;

	public static int base_activity = 0;
	public static int max_activity = 100;
	public static double activity_decay = 0.95;
	public static boolean dynamic_activity = false;

	protected int index_var = 0;

	protected List<String> clone_edges = new ArrayList<>();
	protected Map<Place, Integer> clone_map = new HashMap<Place, Integer>();
	
	protected int voidBlockerVariable = 0;
	protected int voidLimit = 0;
	
	public enum Option {
		SAME_WEIGHT, YUEPENG_WEIGHT, AT_LEAST_ONE, NONE, HARD_AT_LEAST_ONE
	};
	
	protected Option objectiveOption;

	/**
	 * Constructors
	 */
	public PetrinetEncoding(PetriNet p, List<Place> initPlace, Place tgt) {
		petrinet = p;

		inits = initPlace;
		goal = tgt;

		functionMap_vars = new HashMap<>();
		placeMap_vars = new HashMap<>();

		problem_constraints = new ArrayList<Constraint>();
		conflict_constraints = new ArrayList<Constraint>();

		list_variables = new ArrayList<Variable>();
		list_fn_vars = new ArrayList<FunctionVar>();
		list_place_vars = new ArrayList<PlaceVar>();
		objective_function = new Constraint();
		objective_values = new ArrayList<Integer>();

		list_eq_fn_vars = new ArrayList<FunctionVar>();
		map_eq_fn_vars = new HashMap<>();
		map_eq_fn_activity = new HashMap<>();
		model_eq = new ArrayList<>();

		objective_id = 0;
		index_var = 0;
		
		objectiveOption = Option.AT_LEAST_ONE;
	}
	
	public void setObjectiveOption(Option opt){
		objectiveOption = opt;
	}
	
	public Option getObjectiveOption(){
		return objectiveOption;
	}

	public void setBound(int timeline, int tokenNum) {
		maxTimeline = timeline;
		maxToken = tokenNum;
	}

	/**
	 * Public methods
	 */

	public void build() {
		createVariables();
		createConstraints();
		createObjectiveFunctions();
	}

	public List<String> getClones() {
		return clone_edges;
	}

	public void setClones(List<String> clones) {
		clone_edges = clones;
	}
	
	public int getVoidBlocker() {
		return voidBlockerVariable;
	}
	
	public void setVoidBlocker(int a) {
		voidBlockerVariable = a;
	}
	
	public int getVoidLimit(){
		return voidLimit;
	}
	
	public int getTimeline(){
		return maxTimeline;
	}
	
	public Constraint createVoidConstraint(int k) {
		assert petrinet.containsNode("void");
		Place p = petrinet.getPlace("void");
		Constraint c = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(), ConstraintType.LEQ, k, EncodingType.GOAL);
		for (Transition t : p.getPostset()) {

			Pair<Transition, Integer> pair = new Pair<>(t, 0);
			Variable fv = map_eq_fn_vars.get(functionMap_vars.get(pair));
			c.addLiteral(fv, 1);
		}

		Variable voidBlocker = new PlaceVar(index_var++, 0, p, k);
		list_variables.add(voidBlocker);
		c.addLiteral(voidBlocker, -(c.getSize()-k));
		//c.print();
		
		voidBlockerVariable = index_var;
		voidLimit = k;
		
		
		problem_constraints.add(c);
		return c;
	}

	public void createVariables() {
		// index needs to start from 0.
		// create FunctionVar per transition.
		for (Transition t : petrinet.getTransitions()) {
			// temp: creating equivalence variables for transitions
			FunctionVar eq_fv = new FunctionVar(index_var, -1, t);
			list_eq_fn_vars.add(eq_fv);
			list_variables.add(eq_fv);
			map_eq_fn_activity.put(eq_fv, base_activity);
			index_var++;

			for (int tick = 0; tick < maxTimeline; tick++) {
				FunctionVar fv = new FunctionVar(index_var, tick, t);
				Pair<Transition, Integer> pair = new Pair<>(t, tick);
				functionMap_vars.put(pair, fv);
				list_variables.add(fv);
				list_fn_vars.add(fv);
				map_eq_fn_vars.put(fv, eq_fv);
				index_var++;
			}
		}

		// create PlaceVar per place.
		for (Place p : petrinet.getPlaces()) {
			// compute its local maximum token based on isil's theorem.
			int max = 0;
			Set<Flow> edgeSet = new LinkedHashSet<>(p.getPostsetEdges());
			for (Flow outgoing : edgeSet) {
				int weight = outgoing.getWeight();
				if (weight > max)
					max = weight;
			}
			max += 2;
			if (max > maxToken) {
				// assert false : max;
				max = maxToken;
			}

			// checks if the number of initial clones increases Isil's theorem
			int nbClones = 0;
			for (String ss : clone_edges){
				String cloness = "sypet_clone_" + p.getId();
				if (cloness.equals(ss))
					nbClones++;
			}
			
			if (nbClones > 0)
				clone_map.put(p, nbClones);
			
			if (nbClones+2 > max)
				max = nbClones+2;
				
			p.setMaxToken(max);
			
			//restrict max token to 2 for hypergraph.
			if (PathFinder.isHyper && (!inits.contains(p)))
				p.setMaxToken(2);

			for (int tick = 0; tick < maxTimeline; tick++) {
				for (int tokenNum = 0; tokenNum < p.getMaxToken(); tokenNum++) {
					PlaceVar pv = new PlaceVar(index_var, tick, p, tokenNum);
					Trio<Place, Integer, Integer> trio = new Trio<>(p, tick, tokenNum);
					placeMap_vars.put(trio, pv);
					list_variables.add(pv);
					list_place_vars.add(pv);
					index_var++;
				}
			}

		}
		

	}

	public void createConstraints() {

		// 1. constraint for init and goal marking.
		// init marking at t_0.
		/// System.out.println("init marking---------------");

		for (Place p : petrinet.getPlaces()) {

			int tickStart = 0;
			for (int tokenNum = 0; tokenNum < p.getMaxToken(); tokenNum++) {
				Trio<Place, Integer, Integer> trio = new Trio<>(p, tickStart, tokenNum);
				Variable var = placeMap_vars.get(trio);
				assert var != null;
				// @Ruben: this encoding is a bit redundant.
				List<Variable> constraint = new ArrayList<>();
				constraint.add(var);
				// source places are 1 and others are all 0.
				if (inits.contains(p)) {
					
					int initToken = 1;
					if (clone_map.containsKey(p))
						initToken += clone_map.get(p);
					
					if (tokenNum == initToken)
						problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 1, EncodingType.INIT));
					else
						problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 0, EncodingType.INIT));
				} else {
					if (tokenNum == 0)
						problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 1, EncodingType.INIT));
					else
						problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 0, EncodingType.INIT));
				}
			}
		}

		// System.out.println("goal marking---------------");
		// goal marking at t_k.
		for (Place p : petrinet.getPlaces()) {
			int tickStop = maxTimeline - 1;
			for (int tokenNum = 0; tokenNum < p.getMaxToken(); tokenNum++) {
				Trio<Place, Integer, Integer> trio = new Trio<>(p, tickStop, tokenNum);
				Variable var = placeMap_vars.get(trio);
				assert var != null : trio;
				// @Ruben: this encoding is a bit redundant.
				List<Variable> constraint = new ArrayList<>();
				constraint.add(var);
				// goal place is 1 and others are all 0.
				if (p.getId().equals("void")) {
					continue;
				}
				if (p.equals(goal)) {
					if (tokenNum == 1)
						problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 1, EncodingType.GOAL));
					else
						problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 0, EncodingType.GOAL));
				} else {
					if (tokenNum == 0)
						problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 1, EncodingType.GOAL));
					else
						problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 0, EncodingType.GOAL));
				}
			}
		}

		// temp hack: create equivalence between fn variables across iterations
		int pos_eq = 0;

		for (Transition t : petrinet.getTransitions()) {

			List<Variable> constraint = new ArrayList<>();
			List<Integer> coefficients = new ArrayList<>();

			constraint.add(list_eq_fn_vars.get(pos_eq));
			coefficients.add(-1);
			// System.out.print("eq: " + list_eq_fn_vars.get(pos_eq));

			for (int tick = 0; tick < maxTimeline; tick++) {
				Pair<Transition, Integer> pair = new Pair<>(t, tick);
				Variable fv = functionMap_vars.get(pair);
				List<Variable> constraint2 = new ArrayList<>();
				List<Integer> coefficients2 = new ArrayList<>();

				coefficients2.add(-1);
				constraint2.add(fv);
				coefficients2.add(1);
				constraint2.add(list_eq_fn_vars.get(pos_eq));

				coefficients.add(1);
				constraint.add(fv);
				problem_constraints
						.add(new Constraint(constraint2, coefficients2, ConstraintType.GEQ, 0, EncodingType.INIT));

				// System.out.print("fv: " + fv);
			}
			// System.out.println("");
			problem_constraints.add(new Constraint(constraint, coefficients, ConstraintType.GEQ, 0, EncodingType.INIT));
			pos_eq++;
		}
		
		// temp hack: fire at least one transition for each input type that is
		// not void.
		for (Place initPlace : inits) {
			
			if (initPlace.getId().equals("void"))
				continue;

			List<Variable> constraint = new ArrayList<>();
			for (Transition t : initPlace.getPostset()) {
				for (int tick = 0; tick < maxTimeline; tick++) {
					Pair<Transition, Integer> pair = new Pair<>(t, tick);
					Variable fv = functionMap_vars.get(pair);
					constraint.add(fv);
				}
			}
			problem_constraints.add(new Constraint(constraint, ConstraintType.GEQ, 1, EncodingType.INIT));
		}

		// void constraints -- increase the maximum number of transitions that are created from void on demand
		createVoidConstraint(0);

		// temp hack: force initial clone edges
//		int time_step = 1;
//		assert(clone_edges.size() < maxTimeline);
//		for (String s : clone_edges) {
//			if (petrinet.containsTransition(s)) {
//				Transition t = petrinet.getTransition(s);
//				Pair<Transition, Integer> pair = new Pair<>(t, time_step);
//				// System.out.println("pair: " + pair);
//				Set<Place> p = t.getPostset();
//				assert(p.size() == 1);
//				// System.out.println("maxToken: " +
//				// p.iterator().next().getMaxToken());
//				assert(functionMap_vars.containsKey(pair));
//				Variable fv = functionMap_vars.get(pair);
//				Constraint c = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(), ConstraintType.EQ, 1,
//						EncodingType.INIT);
//				c.addLiteral(fv, 1);
//				problem_constraints.add(c);
//				time_step++;
//			}
//		}


		// //add by yu and yuepeng: for each place, only has one possible token
		// number at each time.
		for (int tick = 0; tick < maxTimeline; tick++) {
			for (Place p : petrinet.getPlaces()) {
				List<Variable> constraint = new ArrayList<>();
				for (int tokenNum = 0; tokenNum < p.getMaxToken(); tokenNum++) {
					Trio<Place, Integer, Integer> trio = new Trio<>(p, tick, tokenNum);
					Variable var = placeMap_vars.get(trio);
					constraint.add(var);
				}
				problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 1, EncodingType.INIT));
			}
		}

		// 2. constraint for only one transition could be fired at each time.
		// System.out.println("one transition happen at a time....");
		for (int tick = 0; tick < maxTimeline; tick++) {
			List<Variable> constraint = new ArrayList<>();
			for (Transition t : petrinet.getTransitions()) {
				Pair<Transition, Integer> pair = new Pair<>(t, tick);
				Variable fv = functionMap_vars.get(pair);
				constraint.add(fv);
			}
			// Transaction for t0 are redundant and can be removed -- Ruben
			// add by Yu: no transition is fired at t0.
			if (tick == 0)
				problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 0, EncodingType.ONE_TRANSACTION));
			else
				problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 1, EncodingType.ONE_TRANSACTION));
		}

		// System.out.println("constraint for firing a
		// transition=================");
		// 3. constraint for firing a transition.
		for (Transition t : petrinet.getTransitions()) {
			Set<Flow> postEdges = t.getPostsetEdges();
			// Only one outgoing edge in our domain.
			assert postEdges.size() == 1 : postEdges;
			Flow postEdge = postEdges.iterator().next();
			// Place postPlace = postEdge.getPlace();
			int postWeight = postEdge.getWeight();
			int startTick = 0;

			Set<Flow> preEdges = t.getPresetEdges();
			assert preEdges.size() > 0 : preEdges + " transition: " + t;
			// if t is fired.
			// consume tokens from preset.
			while (startTick < (maxTimeline - 1)) {
				int nextTick = startTick + 1;
				Pair<Transition, Integer> pair = new Pair<>(t, nextTick);
				Variable fv = functionMap_vars.get(pair);

				List<Set<Integer>> list = new ArrayList<>();
				// map place to its corresponding index in the cartesianProduct.
				Map<Integer, Flow> mapIndex = new HashMap<>();
				int index = 0;
				for (Flow pre : preEdges) {
					// min tokens to fire
					int minToken = pre.getWeight();
					Place prePlace = pre.getPlace();
					mapIndex.put(index, pre);
					Set<Integer> set = new LinkedHashSet<>();
					for (int i = minToken; i < prePlace.getMaxToken(); i++) {
						// all possible incoming tokens.
						set.add(i);
					}
					list.add(set);
					index++;
				}
				Set<List<Integer>> products = Sets.cartesianProduct(list);

				// generate constraint for each element in the product result.
				for (List<Integer> productItem : products) {
					List<Variable> constraint = new ArrayList<>();
					List<Integer> coefficint = new ArrayList<>();
					int rhs = 1;

					constraint.add(fv);
					coefficint.add(-1);
					rhs--;
					int cursor = 0;
					for (int pos : productItem) {
						Flow pre = mapIndex.get(cursor);
						assert pre != null;
						Place prePlace = pre.getPlace();
						Trio<Place, Integer, Integer> trioPrev = new Trio<>(prePlace, startTick, pos);
						Variable pvPrev = placeMap_vars.get(trioPrev);
						constraint.add(pvPrev);
						assert pvPrev != null : trioPrev;
						coefficint.add(-1);
						rhs--;
						cursor++;
					}

					cursor = 0;
					for (int pos : productItem) {

						List<Variable> inner_constraint = new ArrayList<Variable>(constraint);
						List<Integer> inner_coefficint = new ArrayList<Integer>(coefficint);

						Flow pre = mapIndex.get(cursor);
						assert pre != null;
						Place prePlace = pre.getPlace();
						int minTokens = pre.getWeight();
						// fire condition.
						assert pos >= minTokens;
						int postTokens = pos - minTokens;

						if (this.hasLoop(pre, prePlace)) {
							postTokens = pos + (t.getPostsetEdges().iterator().next().getWeight() - minTokens);

						}
						// we may need to impose a maximum token limit
						// explicitly -- Ruben
						if (postTokens >= prePlace.getMaxToken()) {
							continue;
						}

						Trio<Place, Integer, Integer> trioPost = new Trio<>(prePlace, nextTick, postTokens);
						Variable pvPost = placeMap_vars.get(trioPost);
						inner_constraint.add(pvPost);
						assert pvPost != null : trioPost + "" + prePlace.getMaxToken();
						inner_coefficint.add(1);
						// this is conjunction, need to split it.
						Constraint c = new Constraint(inner_constraint, inner_coefficint, ConstraintType.GEQ, rhs,
								EncodingType.FIRE_TRANSACTION);
						problem_constraints.add(c);
						// if (t.getId().equals("<java.awt.geom.Area: void
						// transform(java.awt.geom.AffineTransform)>")){
						// System.out.println("psotTokens: " + postTokens);
						// System.out.println("loop: " + this.hasLoop(pre,
						// prePlace));
						// c.print();
						// }
						cursor++;
					}
				}
				startTick++;
			}

			// produce tokens to postset.
			startTick = 0;
			while (startTick < (maxTimeline - 1)) {
				int nextTick = startTick + 1;
				Pair<Transition, Integer> pair = new Pair<>(t, nextTick);
				Variable fv = functionMap_vars.get(pair);

				List<Set<Integer>> list = new ArrayList<>();
				// map place to its corresponding index in the cartesianProduct.
				Map<Integer, Flow> mapIndex = new HashMap<>();
				int index = 0;
				for (Flow pre : preEdges) {
					// min tokens to fire
					int minToken = pre.getWeight();
					Place prePlace = pre.getPlace();
					mapIndex.put(index, pre);
					Set<Integer> set = new LinkedHashSet<>();
					for (int i = minToken; i < prePlace.getMaxToken(); i++) {
						// all possible incoming tokens.
						set.add(i);
					}
					list.add(set);
					index++;
				}

				Set<Integer> postTokenSet = new LinkedHashSet<>();
				Place postPlace = (Place) postEdge.getTarget();
				for (int tgtToken = 0; tgtToken < (postPlace.getMaxToken() - postWeight); tgtToken++) {
					postTokenSet.add(tgtToken);
				}
				list.add(postTokenSet);

				mapIndex.put(index, postEdge);
				Set<List<Integer>> products = Sets.cartesianProduct(list);

				// generate constraint for each element in the product result.
				for (List<Integer> productItem : products) {
					List<Variable> constraint = new ArrayList<>();
					List<Integer> coefficint = new ArrayList<>();
					int rhs = 1;
					constraint.add(fv);
					coefficint.add(-1);
					rhs--;

					int lastIndex = 0;
					boolean hasCircle = false;
					for (int pos : productItem) {
						Flow pre = mapIndex.get(lastIndex);
						assert pre != null : lastIndex + " " + mapIndex.keySet() + " " + productItem;
						Place prePlace = pre.getPlace();

						if (this.hasLoop(pre, prePlace)) {
							hasCircle = true;
							break;
						}

						// last element is the target place, increase its token.
						if (lastIndex == productItem.size() - 1) {
							Trio<Place, Integer, Integer> trioPrev = new Trio<>(prePlace, startTick, pos);
							Variable pvPrev = placeMap_vars.get(trioPrev);
							constraint.add(pvPrev);
							coefficint.add(-1);
							rhs--;

							// increase the token of target place.
							int updateToken = pos + postWeight;

							assert updateToken < prePlace.getMaxToken() : updateToken + " " + prePlace.getMaxToken();
							Trio<Place, Integer, Integer> trioPost = new Trio<>(prePlace, nextTick, updateToken);
							Variable pvPost = placeMap_vars.get(trioPost);
							constraint.add(pvPost);
							coefficint.add(1);
							break;
						}

						Trio<Place, Integer, Integer> trioPrev = new Trio<>(prePlace, startTick, pos);
						Variable pvPrev = placeMap_vars.get(trioPrev);
						constraint.add(pvPrev);
						coefficint.add(-1);
						rhs--;
						lastIndex++;
					}
					assert constraint.size() == coefficint.size();

					if (!hasCircle)
						problem_constraints.add(new Constraint(constraint, coefficint, ConstraintType.GEQ, rhs,
								EncodingType.FIRE_TRANSACTION));
				}
				startTick++;
			}

			// if preconditions are not held, t is not fired.
			startTick = 0;
			while (startTick < (maxTimeline - 1)) {
				int nextTick = startTick + 1;
				for (Flow pre : preEdges) {
					// min tokens to fire
					int minToken = pre.getWeight();
					Place prePlace = pre.getPlace();

					for (int tokenNum = 0; tokenNum < minToken; tokenNum++) {
						List<Variable> constraint = new ArrayList<>();
						List<Integer> coefficient = new ArrayList<>();
						int rhs = 1;

						Pair<Transition, Integer> pair = new Pair<>(t, nextTick);
						Variable fv = functionMap_vars.get(pair);
						assert fv != null : pair;

						Trio<Place, Integer, Integer> trioCurr = new Trio<>(prePlace, startTick, tokenNum);
						Variable pvCurr = placeMap_vars.get(trioCurr);
						assert pvCurr != null : trioCurr + "" + pre;

						constraint.add(fv);
						constraint.add(pvCurr);
						coefficient.add(-1);
						coefficient.add(-1);
						rhs -= 2;
						assert rhs == -1;
						// constraint: \neg(t_nextTick \land
						// x_startTick_tokenNum)
						assert constraint.size() == coefficient.size();
						problem_constraints.add(new Constraint(constraint, coefficient, ConstraintType.GEQ, rhs,
								EncodingType.FIRE_TRANSACTION));
					}
				}
				startTick++;
			}

			// for each transition, dont fire it if postset = maxToken - 1#
			startTick = 0;
			while (startTick < (maxTimeline - 1)) {
				int nextTick = startTick + 1;
				List<Variable> constraint = new ArrayList<>();
				List<Integer> coefficient = new ArrayList<>();
				Pair<Transition, Integer> pair = new Pair<>(t, nextTick);
				Variable fv = functionMap_vars.get(pair);
				assert t.getPostset().size() == 1;
				Place tgt = t.getPostset().iterator().next();
				constraint.add(fv);
				coefficient.add(-1);

				// check if it is a loop that does not increase the postset
				if (tgt == t.getPreset().iterator().next()) {
					if (t.getPostsetEdges().iterator().next().getWeight() == t.getPresetEdges().iterator().next()
							.getWeight()) {
						startTick++;
						continue;
					}
				}

				Trio<Place, Integer, Integer> trioCurr = new Trio<>(tgt, startTick, tgt.getMaxToken() - 1);
				Variable pvCurr = placeMap_vars.get(trioCurr);
				constraint.add(pvCurr);
				coefficient.add(-1);
				int rhs = -1;
				problem_constraints.add(new Constraint(constraint, coefficient, ConstraintType.GEQ, rhs,
						EncodingType.FIRE_TRANSACTION));
				startTick++;
			}

		}
		// System.out.println("constraint for frame axioms =================");
		// 4. constraint for frame axioms per place.
		// for all incoming transition to place p, if none of those transitions
		// are fired, then the tokens of the place remain the same.
		for (Place p : petrinet.getPlaces()) {
			Set<Transition> incomingTransitions = new LinkedHashSet<>();
			incomingTransitions.addAll(p.getPreset());
			// What happens if we have transitions with the same name? -- Ruben
			incomingTransitions.addAll(p.getPostset());

			if (incomingTransitions.isEmpty())
				continue;

			// \neg(f_2 \or f3) \imply (x_token_t_{startTick} \imply
			// x_token_{nextTick})
			for (int tokenNum = 0; tokenNum < p.getMaxToken(); tokenNum++) {
				int startTick = 0;
				while (startTick < (maxTimeline - 1)) {
					int nextTick = startTick + 1;
					List<Variable> constraint = new ArrayList<>();
					List<Integer> coefficient = new ArrayList<>();
					int rhs = 1;

					for (Transition incoming : incomingTransitions) {
						Pair<Transition, Integer> pair = new Pair<>(incoming, nextTick);
						Variable fv = functionMap_vars.get(pair);
						constraint.add(fv);
						coefficient.add(1);
					}

					// x_token_t_{startTick}
					Trio<Place, Integer, Integer> trioCurr = new Trio<>(p, startTick, tokenNum);
					Variable pvCurr = placeMap_vars.get(trioCurr);
					rhs--;
					coefficient.add(-1);
					// negation of pvCurr?
					constraint.add(pvCurr);

					// x_token_{nextTick}
					Trio<Place, Integer, Integer> trioNext = new Trio<>(p, nextTick, tokenNum);
					Variable pvNext = placeMap_vars.get(trioNext);
					constraint.add(pvNext);
					coefficient.add(1);
					assert rhs == 0;

					// equal to: f1 \or f2 \or (\neg x2) \or x2_next
					assert constraint.size() == coefficient.size();
					problem_constraints.add(new Constraint(constraint, coefficient, ConstraintType.GEQ, rhs,
							EncodingType.FRAME_AXIOMS));

					startTick++;
				}
			}

		}
	}

	protected void forceTransition(Transition t){
		Constraint c = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(), ConstraintType.EQ, 1,
				EncodingType.GOAL);
		Pair<Transition, Integer> pair = new Pair<>(t, 0);
		assert (functionMap_vars.containsKey(pair));
		Variable var = functionMap_vars.get(pair);
		assert (map_eq_fn_vars.containsKey(var));
		FunctionVar fv = map_eq_fn_vars.get(var);
		c.addLiteral(fv, 1);
		problem_constraints.add(c);
		
	}
	
	public void createObjectiveFunctions() {
				
		objective_function.getCoefficients().clear();
		objective_function.getLiterals().clear();

		//Option opt = Option.YUEPENG_WEIGHT;
		Option opt = objectiveOption;
		//System.out.println("opt: " + objectiveOption);

		Constraint c = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(), ConstraintType.GEQ, 1,
				EncodingType.GOAL);

		// prefer the transitions from yuepeng list
		for (FunctionVar fv : list_eq_fn_vars) {
			if (!map_eq_fn_activity.containsKey(fv))
				continue;
			assert(map_eq_fn_activity.containsKey(fv));

			if (map_eq_fn_activity.get(fv) != 0) {

				if (opt == Option.SAME_WEIGHT) {
					objective_function.addLiteral(fv, -1);
				} else if (opt == Option.YUEPENG_WEIGHT) {
					objective_function.addLiteral(fv, map_eq_fn_activity.get(fv));
				} else if (opt == Option.AT_LEAST_ONE || opt == Option.HARD_AT_LEAST_ONE) {
					c.addLiteral(fv, 1);
				}
			}
		}

		if (opt == Option.AT_LEAST_ONE || opt == Option.SAME_WEIGHT) {
			FunctionVar eq_obj = new FunctionVar(index_var++, -1, null);
			list_variables.add(eq_obj);

			c.addLiteral(eq_obj, 1);
			problem_constraints.add(c);

			objective_function.addLiteral(eq_obj, 100);
		}
		
		if (opt == Option.YUEPENG_WEIGHT){
			FunctionVar eq_obj = new FunctionVar(index_var++, -1, null);
			list_variables.add(eq_obj);

			c.addLiteral(eq_obj, 1);
			problem_constraints.add(c);

			objective_function.addLiteral(eq_obj, 1000);
		}

		if (opt == Option.HARD_AT_LEAST_ONE) {
			problem_constraints.add(c);
		}
		
		if (opt != Option.NONE) {
			// if (false) {

			// do not prefer transitions that come or go to void
			if (petrinet.containsPlace("void")) {
				Place voidP = petrinet.getPlace("void");
				Constraint voidPostset = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(),
						ConstraintType.LEQ, 0, EncodingType.INIT);

				for (Transition t : voidP.getPostset()) {
					Pair<Transition, Integer> pair = new Pair<>(t, 0);
					if (!functionMap_vars.containsKey(pair))
						continue;
					assert functionMap_vars.containsKey(pair) : pair;
					Variable var = functionMap_vars.get(pair);
					assert map_eq_fn_vars.containsKey(var);
					FunctionVar fv = map_eq_fn_vars.get(var);
					voidPostset.addLiteral(fv, 1);
				}

				if (voidPostset.getSize() > 0) {
					FunctionVar eq_voidpostset = new FunctionVar(index_var++, -1, null);
					list_variables.add(eq_voidpostset);

					voidPostset.addLiteral(eq_voidpostset, -voidPostset.getSize());
					problem_constraints.add(voidPostset);
					objective_function.addLiteral(eq_voidpostset, 1);
				}

				Constraint voidPreset = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(),
						ConstraintType.LEQ, 0, EncodingType.INIT);
				for (Transition t : voidP.getPreset()) {
					Pair<Transition, Integer> pair = new Pair<>(t, 0);
					if (!functionMap_vars.containsKey(pair))
						continue;
					assert functionMap_vars.containsKey(pair);
					Variable var = functionMap_vars.get(pair);
					assert map_eq_fn_vars.containsKey(var);
					FunctionVar fv = map_eq_fn_vars.get(var);
					voidPreset.addLiteral(fv, 1);
				}

				if (voidPreset.getSize() > 0) {
					FunctionVar eq_voidpreset = new FunctionVar(index_var++, -1, null);
					list_variables.add(eq_voidpreset);

					voidPreset.addLiteral(eq_voidpreset, -voidPreset.getSize());
					problem_constraints.add(voidPreset);
					objective_function.addLiteral(eq_voidpreset, 1);
				}
			}

			// do not prefer transitions that do clone
			Constraint cloneCtr = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(),
					ConstraintType.LEQ, 0, EncodingType.INIT);

			for (Transition t : petrinet.getTransitions()) {
				if (t.getId().startsWith("sypet_clone_")) {

					Pair<Transition, Integer> pair = new Pair<>(t, 0);
					assert functionMap_vars.containsKey(pair) : pair + " : " + maxTimeline
							+ petrinet.containsTransition(t);
					Variable var = functionMap_vars.get(pair);
					assert map_eq_fn_vars.containsKey(var);
					FunctionVar fv = map_eq_fn_vars.get(var);
					cloneCtr.addLiteral(fv, 1);

				}
			}

			if (cloneCtr.getSize() > 0) {
				FunctionVar eq_clone = new FunctionVar(index_var++, -1, null);
				list_variables.add(eq_clone);

				cloneCtr.addLiteral(eq_clone, -cloneCtr.getSize());
				problem_constraints.add(cloneCtr);
				objective_function.addLiteral(eq_clone, 1);
			}
			
			domainHeuristics();
		}
	}
	
	protected void domainHeuristics() {
		
//		ArrayList<Transition> preferences = new ArrayList<>();
//		for (Transition t : petrinet.getTransitions()){
//			if (t.getId().contains("withMaximumValue")){
//				preferences.add(t);
//			}
//		}
//
//		if (!preferences.isEmpty()) {
//			Constraint dom = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(), ConstraintType.GEQ, 1);
//			for (Transition tt : preferences) {
//
//				Pair<Transition, Integer> pairt = new Pair<>(tt, 0);
//				FunctionVar vart = map_eq_fn_vars.get(functionMap_vars.get(pairt));
//
//				dom.addLiteral(vart, 1);
//			}
//
//			FunctionVar eqdom = new FunctionVar(index_var++, -1, null);
//			list_variables.add(eqdom);
//			dom.addLiteral(eqdom, dom.getSize());
//
//			problem_constraints.add(dom);
//			objective_function.addLiteral(eqdom, 1);
//		}		
		
		ArrayList<Transition> betweenTransition = new ArrayList<>();
		for (Transition tt : petrinet.getTransitions()){
			// Between methods
			if (tt.getId().contains("Between")){
				betweenTransition.add(tt);
			}
			
		}
		
		// Minor preference for between methods
		if (!betweenTransition.isEmpty()) {
			Constraint dom = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(), ConstraintType.GEQ, 1);
			for (Transition tt : betweenTransition) {

				Pair<Transition, Integer> pairt = new Pair<>(tt, 0);
				FunctionVar vart = map_eq_fn_vars.get(functionMap_vars.get(pairt));

				dom.addLiteral(vart, 1);
			}	
			
			FunctionVar eqdom = new FunctionVar(index_var++, -1, null);
			list_variables.add(eqdom);
			dom.addLiteral(eqdom, dom.getSize());
			
			problem_constraints.add(dom);
			objective_function.addLiteral(eqdom, 10);
		}
		
		// Domain heuristic for point : if getX is used then we prefer getY and vice versa
		for (Place initPlace : inits) {

			if (initPlace.getId().equals("point.Point") || initPlace.getId().equals("point.MyPoint")) {
				ArrayList<Variable> getters = new ArrayList<Variable>();
				for (Transition t : initPlace.getPostset()) {
					if (!t.getId().contains("getX") && !t.getId().contains("getY"))
						continue;

					Pair<Transition, Integer> pair = new Pair<>(t, 0);
					Variable fv = functionMap_vars.get(pair);
					getters.add(map_eq_fn_vars.get(fv));
				}

				if (!getters.isEmpty()) {

					FunctionVar eqPoint2D = new FunctionVar(index_var++, -1, null);
					list_variables.add(eqPoint2D);
					Constraint ato = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(),
							ConstraintType.GEQ, 1);
					for (int i = 0; i < getters.size(); i++) {
						ato.addLiteral(getters.get(i), 1);
						for (int j = 0; j < getters.size(); j++) {
							if (i == j)
								continue;

							Constraint req = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(),
									ConstraintType.GEQ, 0);
							req.addLiteral(getters.get(i), -1);
							req.addLiteral(getters.get(j), 1);
							req.addLiteral(eqPoint2D, 1);
							problem_constraints.add(req);

							Constraint leq = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(),
									ConstraintType.GEQ, 0);
							leq.addLiteral(getters.get(i), 1);
							leq.addLiteral(getters.get(j), -1);
							leq.addLiteral(eqPoint2D, 1);
							problem_constraints.add(leq);

						}
					}
					ato.addLiteral(eqPoint2D, 1);
					problem_constraints.add(ato);
					objective_function.addLiteral(eqPoint2D, 100);
				}
			}
		}
		
		// Domain heuristic for point : if getX is used then we prefer getY and vice versa
		for (Place initPlace : inits) {

			if (initPlace.getId().equals("java.awt.geom.Point2D") || initPlace.getId().equals("java.awt.Point")) {
				ArrayList<Variable> getters = new ArrayList<Variable>();
				for (Transition t : initPlace.getPostset()) {
					if (!t.getId().contains("getX") && !t.getId().contains("getY"))
						continue;

					Pair<Transition, Integer> pair = new Pair<>(t, 0);
					Variable fv = functionMap_vars.get(pair);
					getters.add(map_eq_fn_vars.get(fv));
				}

				if (!getters.isEmpty()) {

					FunctionVar eqPoint2D = new FunctionVar(index_var++, -1, null);
					list_variables.add(eqPoint2D);
					Constraint ato = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(),
							ConstraintType.GEQ, 1);
					for (int i = 0; i < getters.size(); i++) {
						ato.addLiteral(getters.get(i), 1);
						for (int j = 0; j < getters.size(); j++) {
							if (i == j)
								continue;

							Constraint req = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(),
									ConstraintType.GEQ, 0);
							req.addLiteral(getters.get(i), -1);
							req.addLiteral(getters.get(j), 1);
							req.addLiteral(eqPoint2D, 1);
							problem_constraints.add(req);

							Constraint leq = new Constraint(new ArrayList<Variable>(), new ArrayList<Integer>(),
									ConstraintType.GEQ, 0);
							leq.addLiteral(getters.get(i), 1);
							leq.addLiteral(getters.get(j), -1);
							leq.addLiteral(eqPoint2D, 1);
							problem_constraints.add(leq);

						}
					}
					ato.addLiteral(eqPoint2D, 1);
					problem_constraints.add(ato);
					objective_function.addLiteral(eqPoint2D, 100);
				}
			}
		}
		
	}

	public List<Integer> getObjectiveValues() {
		return objective_values;
	}

	public void setObjectiveId(int objective_id) {
		this.objective_id = objective_id;
	}

	public List<Constraint> getConflictConstraints() {
		return conflict_constraints;
	}

	public Integer nVars() {
		return list_variables.size();
	}

	public List<Variable> getVariables() {
		return list_variables;
	}

	public Integer nConstraints() {
		return problem_constraints.size();
	}

	public List<Constraint> getConstraints() {
		return problem_constraints;
	}

	public Constraint getObjectiveFunctions() {
		return objective_function;
	}

	public Integer getObjectiveId() {
		return objective_id;
	}

	public boolean hasLoop(Flow e, Place p) {
		Transition t = e.getTransition();
		if (t.getPostsetEdges().size() < 1)
			return false;

		assert t.getPostset().size() == 1;
		Set<Node> srcNodes = t.getPresetNodes();
		Place postPlace = t.getPostset().iterator().next();
		// p appears as both input and output.
		boolean flag = srcNodes.contains(p) && p.equals(postPlace);
		return flag;
	}

	public List<String> saveModel(Solver solver) {
		List<FunctionVar> list = new ArrayList<>();
		List<String> res = new ArrayList<>();

		for (int i = 0; i < list_fn_vars.size(); i++) {
			FunctionVar var = list_fn_vars.get(i);
			if (!solver.getModel().get(var.getSolverId()))
				continue;

			list.add(var);
			Collections.sort(list, new Comparator<FunctionVar>() {
				public int compare(FunctionVar s1, FunctionVar s2) {
					return s1.time - s2.time;
				}
			});
		}

		model_eq = list;

		for (FunctionVar v : list) {
			res.add(v.getTransition().getId());
		}
		return res;
	}

	public void increaseActivity(FunctionVar fv, int value) {
		assert(map_eq_fn_activity.containsKey(fv));
		map_eq_fn_activity.put(fv, value);
	}

	public Constraint blockPath() {
		assert(!model_eq.isEmpty());

		Map<FunctionVar, Integer> occ_eq = new HashMap<>();
		ArrayList<Variable> constraint = new ArrayList<>();
		ArrayList<Integer> coefficients = new ArrayList<>();

		for (FunctionVar v : model_eq) {
			FunctionVar eq_v = map_eq_fn_vars.get(v);
			if (occ_eq.containsKey(eq_v)) {
				occ_eq.put(eq_v, occ_eq.get(eq_v) + 1);
			} else {
				occ_eq.put(eq_v, 1);
			}
		}

		boolean duplicate = false;
		for (FunctionVar key : occ_eq.keySet()) {
			constraint.add(key);
			coefficients.add(-occ_eq.get(key));
			if (occ_eq.get(key) > 1) {
				duplicate = true;
			}
		}

		// Update activity
		// if (dynamic_activity) {
		// for (FunctionVar fv : list_eq_fn_vars) {
		// if (occ_eq.containsKey(fv)) {
		// int before = map_eq_fn_activity.get(fv);
		// map_eq_fn_activity.put(fv, (int) Math
		// .ceil((double) before * activity_decay + max_activity - max_activity
		// * activity_decay));
		// } else {
		// int before = map_eq_fn_activity.get(fv);
		// map_eq_fn_activity.put(fv, (int) Math.ceil((double) before *
		// activity_decay));
		// }
		// }
		//
		// createObjectiveFunctions();
		// }

		// duplicate = true;
		if (!duplicate) {
			// problem_constraints.add(new Constraint(constraint, coefficients,
			// ConstraintType.GEQ,
			// -(constraint.size() - 1), EncodingType.GOAL));
			model_eq.clear();
			return new Constraint(constraint, coefficients, ConstraintType.GEQ, -(constraint.size() - 1),
					EncodingType.GOAL);
		} else {
			//System.out.println("SUM2!");
			constraint.clear();
			coefficients.clear();
			for (FunctionVar v : model_eq) {
				constraint.add(v);
				coefficients.add(-1);
				// System.out.println("model: " + v);
			}
			model_eq.clear();
			// problem_constraints.add(new Constraint(constraint, coefficients,
			// ConstraintType.GEQ,
			// -(constraint.size() - 1), EncodingType.GOAL));
			return new Constraint(constraint, coefficients, ConstraintType.GEQ, -(constraint.size() - 1),
					EncodingType.GOAL);
		}

	}

	public void print() {

		System.out.println("#Variables: " + nVars());
		System.out.println("#Constraints: " + nConstraints());

		for (Constraint c : problem_constraints) {
			c.print();
		}
	}

	public void print(String s, EncodingType enctype) {

		System.out.println(s);
		for (Constraint c : problem_constraints) {
			if (c.getEncodingType() != enctype)
				continue;

			c.print();
		}

	}

	public void printVariables() {

		System.out.println("list_fn_vars: " + list_fn_vars.size());

		for (FunctionVar fv : list_fn_vars) {
			System.out.println(fv.toString());
		}

		System.out.println("list_place_vars: " + list_place_vars.size());

		for (PlaceVar pv : list_place_vars) {
			System.out.println(pv.toString());
		}

	}

	public void reset() {
		problem_constraints.clear();
		conflict_constraints.clear();
		list_variables.clear();

		objective_values.clear();
		objective_id = 0;
	}
	
	public PetriNet getPetriNet() {
		return petrinet;
	}
	
}