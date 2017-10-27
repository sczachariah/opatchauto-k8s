from opatchautofmwqa_simpleproduct import SimpleProduct
from opatchautofmwqa_test_em import TestEM
from opatchautofmwqa_set_ds_timeout import SetDSTimeout



def sayHello(envmodel, targets, extras) :
    simpleProduct = SimpleProduct()
    return simpleProduct.sayHello(envmodel, targets, extras)

def sayHowAreYou(envmodel, targets, extras) :
    simpleProduct = SimpleProduct()
    return simpleProduct.sayHowAreYou(envmodel, targets, extras)

def updateEMApp(envmodel, targets, extras) :
    testEM = TestEM()
    return testEM.updateEMApp(envmodel, targets, extras)

def updateDS(envmodel, targets, extras) :
    setDSTimeout = SetDSTimeout()
    return setDSTimeout.updateDS(envmodel, targets, extras)
