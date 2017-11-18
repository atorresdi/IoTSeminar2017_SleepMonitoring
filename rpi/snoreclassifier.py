#Imports
from python_speech_features import mfcc
from python_speech_features import delta
from python_speech_features import logfbank
import scipy.io.wavfile as wav
import csv
import numpy as np

class SnoreClassifier:

    def __init__(self):

        self.KNN_THRESHOLD = 90

        self.NUM_SNORES = 132   #135 actual trainings, rest are recorded

        self.NUM_COUGHS = 10
        self.NUM_TRAFFIC = 10

        self.NUM_NOT_SNORES = self.NUM_COUGHS + self.NUM_TRAFFIC + 10
        self.NUM_FILES = self.NUM_SNORES + self.NUM_NOT_SNORES

        self.MFCC_FEATS = [1, 10]
        self.NUM_FEATS = len(self.MFCC_FEATS)*3        #multiplied by 3 because of [max, min, avg]

        self.knnData = np.empty((self.NUM_SNORES, self.NUM_FEATS), dtype=float, order='C')        #Snoring MFCC coefficients

    ##########################################################################################

    def initialiseData(self):
        #Get Snoring data
        for i in range(10,self.NUM_SNORES):
            (fs,sig) = wav.read("FakeSnores/{0}.wav".format(i))
            for j in range(len(self.MFCC_FEATS)):
                self.knnData[i,j*3:j*3+3] = self.getMfccMMA(fs, sig, self.MFCC_FEATS[j])

    ##########################################################################################
    def getMfccMMA(self, fs, sig, mfccFeat):
        channel1 = np.empty((len(sig),1), dtype=float, order='C')
        
        if (len(sig.shape)>1):
            channel1 = sig[:,0]     #Change Stereo to Mono channel
        else:
            channel1 = sig
            
        mfcc_feat = mfcc(channel1, samplerate=fs, nfft=2048, preemph=0.95)
        
        return [self.maximum(mfcc_feat[:,mfccFeat]), self.minimum(mfcc_feat[:,mfccFeat]),self.average(mfcc_feat[:,mfccFeat])]

    ##########################################################################################
    def calcDiff(self, val1,val2):
        diff = (val1 - val2)
        if(diff < 0):
            diff *= -1
        return diff
    ##########################################################################################

    def isSnore(self,fileName):
        knnDataNS = np.empty((self.NUM_FEATS), dtype=float, order='C')  #Not Snoring MFCC coefficients
        diff = [0]*self.NUM_FEATS    #difference between training and test for MFCC 1[max, min, avg] and MFCC 11 [max, min, avg]
        count = 0
        minIndDist = 4000000
        
        (fs,sig) = wav.read("{0}.wav".format(fileName))
        for j in range(len(self.MFCC_FEATS)):
            knnDataNS[j*3:j*3+3] = self.getMfccMMA(fs, sig, self.MFCC_FEATS[j])

        for j in range(self.NUM_SNORES):                          #range of training
            indDist = 0                                      #Individual Distance for single test data point, squared
            for i in range(self.NUM_FEATS):
                diff[i] = self.calcDiff(self.knnData[j,i], knnDataNS[i])

                #Get dist^2
                indDist += diff[i]*diff[i]
                
            if(minIndDist > indDist):
                minIndDist = indDist
        
            if(minIndDist < self.KNN_THRESHOLD):
                print('Coeff: ',minIndDist)  #If there are some incorrect classifications then you can check this to set a new threshold
                count += 1
                minIndDist = 4000000
                if(count > 2):
                    return 1           #Snore
        if(minIndDist < self.KNN_THRESHOLD):
            pass
            #print('Coeff: ',minIndDist)  #If there are some incorrect classifications then you can check this to set a new threshold
            #return 1           #Snore 
        else:
            print('Coeff: ',minIndDist)
            print('Count: ', count)
            return 0           #Not snore
    ##########################################################################################

    def average(self, sig):
        sum = 0

        if(sig.size > 1):
            for i in range(sig.size):
                sum += sig[i]
            return (sum/sig.size)
        else:
            return sig
    ##########################################################################################
    def maximum(self, sig):
        max = 0

        if(sig.size > 1):
            for i in range(len(sig)):
                if(max < sig[i]):
                    max = sig[i]
        else:
            max = sig
        return max
    ##########################################################################################
    def minimum(self, sig):
        min = 4000000

        if(sig.size > 1):
            for i in range(sig.size):
                if(min > sig[i]):
                    min = sig[i]
        else:
            min = sig
        return min
    ##########################################################################################    
