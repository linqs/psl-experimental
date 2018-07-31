#!/usr/bin/env python3

'''
This file is part of the PSL software.
Copyright 2011-2015 University of Maryland
Copyright 2013-2018 The Regents of the University of California

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import json
import os
import sys
import time

import cvxpy

LOG_PREFIX = 'cvxpy_reasoner'

def getNowMS():
    return int(round(time.time() * 1000))

def log(text):
    print("%s - %s" % (LOG_PREFIX, text) )

def readProblem(inputPath):
    with open(inputPath, 'r') as file:
        return json.load(file)

# Takes ownership of |variableMap|.
def writeResults(outputPath, variableMap):
    # Overwrite the cvxpy variable with its value.
    for variableName, variable in variableMap.items():
        variableMap[variableName] = variable.value

    output = {
        'solution': variableMap
    }

    with open(outputPath, 'w') as file:
        json.dump(output, file)

# Returns: {variableName: variableObject, ...}
def initVariables(problem):
    variableMap = {}

    for variable in problem['variables']:
        variableMap[variable] = cvxpy.Variable()

    return variableMap

def buildConstraints(problem, variableMap):
    constraints = []

    # Constrain variables.
    for _, variable in variableMap.items():
        constraints += [0.0 <= variable, variable <= 1.0]

    for term in problem['constraints']:
        localObjective = buildLocalObjective(term, variableMap)
        constraints.append(localObjective <= 0.0)

    return constraints

def buildLocalObjective(term, variableMap):
    localObjective = term['constant']

    for i in range(len(term['variables'])):
        localObjective += (term['coefficients'][i] * variableMap[term['variables'][i]])

    return localObjective

def buildObjective(problem, variableMap):
    objective = 0.0

    for term in problem['objectiveSummands']:
        localObjective = buildLocalObjective(term, variableMap)
        localObjective = cvxpy.pos(localObjective)

        if (term['squared']):
            localObjective = cvxpy.square(localObjective)

        objective += (term['weight'] * localObjective)

    return objective

def main(inputPath, outputPath):
    now = getNowMS()
    log("Reading problem")
    problem = readProblem(inputPath)
    log("Problem reading complete in %d ms" % (getNowMS() - now))
    
    now = getNowMS()
    log("Initializing problem")
    variableMap = initVariables(problem)
    constraints = buildConstraints(problem, variableMap)
    objective = buildObjective(problem, variableMap)
    log("Problem initializion complete in %d ms" % (getNowMS() - now))

    now = getNowMS()
    log("Begining optimization")
    optimizationProblem = cvxpy.Problem(cvxpy.Minimize(objective), constraints)
    optimizationProblem.solve()
    log("Optimzation complete in %d ms" % (getNowMS() - now))

    now = getNowMS()
    log("Writing results")
    problem = writeResults(outputPath, variableMap)
    log("Result writing complete in %d ms" % (getNowMS() - now))

def loadArgs(args):
    if (len(args) != 3 or 'help' in [arg.lower().strip().replace('-', '') for arg in args]):
        print("USAGE: python3 %s <input path> <output path>" % (args[0]))
        print("   input path  - the path to a JSON file describing the optimization problem.")
        print("   output path - the path where the solution will be written.")
        sys.exit(1)

    inputPath = os.path.abspath(args.pop(1))
    outputPath = os.path.abspath(args.pop(1))

    if (not os.path.isfile(inputPath)):
        raise "Input path does not exist: [%s]." % (inputPath)

    os.makedirs(os.path.dirname(outputPath), exist_ok = True)

    return inputPath, outputPath

if __name__ == '__main__':
    main(*loadArgs(sys.argv))
